package com.phi.prescription;

import com.phi.domain.MedicationCourseType;
import java.time.LocalDate;
import java.util.List;

public final class PrescriptionDtos {

    private PrescriptionDtos() {
    }

    public record ExtractedMedicationItem(
            String medicationName,
            String dosage,
            String scheduleText,
            LocalDate startedOn,
            Integer durationDays,
            MedicationCourseType courseType
    ) {
    }

    public record PrescriptionItemsResponse(
            Long reportId,
            String reportDate,
            List<ExtractedMedicationItem> items
    ) {
    }

    public record ConfirmMedicationItem(
            String medicationName,
            String dosage,
            String scheduleText,
            LocalDate startedOn,
            Integer durationDays,
            String courseType
    ) {
    }

    public record ConfirmMedicationsRequest(List<ConfirmMedicationItem> medications) {
    }

    public record ConfirmMedicationsResponse(int savedCount, List<String> medicationIds) {
    }
}
