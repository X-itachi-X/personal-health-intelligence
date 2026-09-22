package com.phi.golden;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoldenPrescriptionFixture(
        String patient,
        @JsonProperty("report_date") String reportDate,
        String source,
        @JsonProperty("source_text") String sourceText,
        List<ExpectedMedication> medications
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExpectedMedication(
            @JsonProperty("medication_name") String medicationName,
            String dosage,
            @JsonProperty("schedule_text") String scheduleText,
            @JsonProperty("course_type") String courseType,
            @JsonProperty("duration_days") Integer durationDays
    ) {
    }
}
