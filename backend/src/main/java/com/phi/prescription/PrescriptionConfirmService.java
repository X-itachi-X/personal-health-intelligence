package com.phi.prescription;

import com.phi.access.AccessControlService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.ExtractionStatus;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.MedicationCourseType;
import com.phi.domain.MedicationEvent;
import com.phi.domain.MedicationEventRepository;
import com.phi.domain.MedicationSource;
import com.phi.domain.Person;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PrescriptionConfirmService {

    private final LabReportRepository labReportRepository;
    private final MedicationEventRepository medicationEventRepository;
    private final AccessControlService accessControl;

    public PrescriptionConfirmService(
            LabReportRepository labReportRepository,
            MedicationEventRepository medicationEventRepository,
            AccessControlService accessControl
    ) {
        this.labReportRepository = labReportRepository;
        this.medicationEventRepository = medicationEventRepository;
        this.accessControl = accessControl;
    }

    @Transactional
    public PrescriptionDtos.ConfirmMedicationsResponse confirm(
            AuthenticatedAccount account,
            Long reportId,
            PrescriptionDtos.ConfirmMedicationsRequest request
    ) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        accessControl.requireCanUploadFor(account, report.getFamilyId(), report.getPerson().getId());

        if (!report.isPrescription()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report is not a prescription upload");
        }
        if (report.getExtractionStatus() != ExtractionStatus.AWAITING_MEDICATION_CONFIRMATION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Prescription is not awaiting confirmation");
        }
        if (request.medications() == null || request.medications().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one medication is required");
        }

        Person person = report.getPerson();
        List<String> savedIds = new ArrayList<>();
        for (PrescriptionDtos.ConfirmMedicationItem item : request.medications()) {
            if (item.medicationName() == null || item.medicationName().isBlank()) {
                continue;
            }
            LocalDate startedOn = item.startedOn() != null ? item.startedOn() : report.getReportDate();
            if (startedOn == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startedOn is required for each medication");
            }
            MedicationCourseType courseType = parseCourseType(item.courseType());
            LocalDate expectedEndOn = expectedEndOn(startedOn, item.durationDays(), courseType);

            MedicationEvent event = new MedicationEvent(
                    person,
                    report.getFamilyId(),
                    item.medicationName().trim(),
                    normalize(item.dosage()),
                    startedOn,
                    null,
                    null,
                    courseType,
                    normalize(item.scheduleText()),
                    item.durationDays(),
                    expectedEndOn,
                    MedicationSource.PRESCRIPTION,
                    report,
                    account.accountId()
            );
            savedIds.add(medicationEventRepository.save(event).getId());
        }

        if (savedIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid medications to save");
        }

        report.markCompleted(report.getExtractedText());
        labReportRepository.save(report);

        return new PrescriptionDtos.ConfirmMedicationsResponse(savedIds.size(), savedIds);
    }

    private static MedicationCourseType parseCourseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return MedicationCourseType.UNKNOWN;
        }
        try {
            return MedicationCourseType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MedicationCourseType.UNKNOWN;
        }
    }

    private static LocalDate expectedEndOn(LocalDate startedOn, Integer durationDays, MedicationCourseType courseType) {
        if (courseType == MedicationCourseType.ACUTE && durationDays != null && durationDays > 0) {
            return startedOn.plusDays(durationDays - 1L);
        }
        return null;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
