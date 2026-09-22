package com.phi.imaging;

import com.phi.access.AccessControlService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.ImagingFinding;
import com.phi.domain.ImagingFindingRepository;
import com.phi.domain.ImagingStudy;
import com.phi.domain.ImagingStudyRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImagingQueryService {

    private final ImagingStudyRepository imagingStudyRepository;
    private final ImagingFindingRepository imagingFindingRepository;
    private final AccessControlService accessControl;

    public ImagingQueryService(
            ImagingStudyRepository imagingStudyRepository,
            ImagingFindingRepository imagingFindingRepository,
            AccessControlService accessControl
    ) {
        this.imagingStudyRepository = imagingStudyRepository;
        this.imagingFindingRepository = imagingFindingRepository;
        this.accessControl = accessControl;
    }

    @Transactional(readOnly = true)
    public ImagingDtos.StudyView getForPerson(AuthenticatedAccount account, Long personId, String studyId) {
        accessControl.requireAuthenticated(account);
        if (!personId.equals(account.personId())) {
            accessControl.requireAdvanced(account);
            if (!accessControl.sameFamily(account.personId(), personId)) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN
                );
            }
        }

        ImagingStudy study = imagingStudyRepository.findById(studyId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Imaging study not found"
                ));
        if (!study.getPerson().getId().equals(personId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Imaging study not found"
            );
        }
        return toView(study);
    }

    @Transactional(readOnly = true)
    public List<ImagingDtos.StudyView> listForPerson(AuthenticatedAccount account, Long personId) {
        accessControl.requireAuthenticated(account);
        if (!personId.equals(account.personId())) {
            accessControl.requireAdvanced(account);
            if (!accessControl.sameFamily(account.personId(), personId)) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN
                );
            }
        }

        return imagingStudyRepository.findByPersonIdOrderByStudyDateDesc(personId).stream()
                .map(this::toView)
                .toList();
    }

    private ImagingDtos.StudyView toView(ImagingStudy study) {
        List<ImagingFinding> findings = imagingFindingRepository.findByImagingStudyIdOrderBySortOrderAsc(study.getId());
        return new ImagingDtos.StudyView(
                study.getId(),
                study.getPerson().getId(),
                study.getModality().name(),
                study.getBodyRegion(),
                study.getStudyDate().toString(),
                study.getFacility(),
                study.getImpression(),
                study.getSourceReport() != null ? study.getSourceReport().getId() : null,
                findings.stream()
                        .map(finding -> new ImagingDtos.FindingView(
                                finding.getId(),
                                finding.getFindingText(),
                                finding.getSeverity(),
                                finding.getMeasurementValue(),
                                finding.getMeasurementUnit()
                        ))
                        .toList()
        );
    }
}
