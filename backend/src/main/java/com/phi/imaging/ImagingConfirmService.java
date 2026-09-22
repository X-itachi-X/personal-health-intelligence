package com.phi.imaging;

import com.phi.access.AccessControlService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.ExtractionStatus;
import com.phi.domain.ImagingFinding;
import com.phi.domain.ImagingFindingRepository;
import com.phi.domain.ImagingModality;
import com.phi.domain.ImagingStudy;
import com.phi.domain.ImagingStudyRepository;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.Person;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ImagingConfirmService {

    private final LabReportRepository labReportRepository;
    private final ImagingStudyRepository imagingStudyRepository;
    private final ImagingFindingRepository imagingFindingRepository;
    private final AccessControlService accessControl;

    public ImagingConfirmService(
            LabReportRepository labReportRepository,
            ImagingStudyRepository imagingStudyRepository,
            ImagingFindingRepository imagingFindingRepository,
            AccessControlService accessControl
    ) {
        this.labReportRepository = labReportRepository;
        this.imagingStudyRepository = imagingStudyRepository;
        this.imagingFindingRepository = imagingFindingRepository;
        this.accessControl = accessControl;
    }

    @Transactional
    public ImagingDtos.ConfirmImagingResponse confirm(
            AuthenticatedAccount account,
            Long reportId,
            ImagingDtos.ConfirmImagingRequest request
    ) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        accessControl.requireCanUploadFor(account, report.getFamilyId(), report.getPerson().getId());

        if (!report.isImagingReport()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report is not an imaging upload");
        }
        if (report.getExtractionStatus() != ExtractionStatus.AWAITING_IMAGING_CONFIRMATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Imaging report is not awaiting confirmation");
        }

        LocalDate studyDate = request.studyDate() != null ? request.studyDate() : report.getReportDate();
        if (studyDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studyDate is required");
        }
        if (request.impression() == null || request.impression().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "impression is required");
        }

        Person person = report.getPerson();
        ImagingStudy study = new ImagingStudy(
                person,
                report.getFamilyId(),
                report,
                parseModality(request.modality()),
                normalize(request.bodyRegion()),
                studyDate,
                normalize(request.facility()),
                request.impression().trim(),
                account.accountId()
        );
        study = imagingStudyRepository.save(study);

        List<ImagingDtos.ConfirmFindingItem> findings = request.findings() != null ? request.findings() : List.of();
        int order = 0;
        for (ImagingDtos.ConfirmFindingItem item : findings) {
            if (item.findingText() == null || item.findingText().isBlank()) {
                continue;
            }
            imagingFindingRepository.save(new ImagingFinding(
                    study,
                    item.findingText().trim(),
                    normalize(item.severity()),
                    normalize(item.measurementValue()),
                    normalize(item.measurementUnit()),
                    order++
            ));
        }

        report.markCompleted(report.getExtractedText());
        labReportRepository.save(report);

        return new ImagingDtos.ConfirmImagingResponse(study.getId());
    }

    private static ImagingModality parseModality(String raw) {
        if (raw == null || raw.isBlank()) {
            return ImagingModality.OTHER;
        }
        try {
            return ImagingModality.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ImagingModality.OTHER;
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
