package com.phi.medication;

import java.time.LocalDate;

public final class MedicationDtos {

    private MedicationDtos() {
    }

    public record CreateMedicationRequest(
            String medicationName,
            String dosage,
            LocalDate startedOn,
            LocalDate endedOn,
            String notes,
            String courseType,
            String scheduleText,
            Integer durationDays,
            LocalDate expectedEndOn
    ) {
    }

    public record EndMedicationRequest(LocalDate endedOn) {
    }

    public record MedicationView(
            String id,
            Long personId,
            String medicationName,
            String dosage,
            String startedOn,
            String endedOn,
            String notes,
            String courseType,
            String scheduleText,
            Integer durationDays,
            String expectedEndOn,
            String source,
            Long sourceReportId,
            boolean active
    ) {
    }
}
