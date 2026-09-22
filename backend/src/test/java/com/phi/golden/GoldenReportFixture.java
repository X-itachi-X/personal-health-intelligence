package com.phi.golden;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoldenReportFixture(
        String patient,
        @JsonProperty("report_date") String reportDate,
        String source,
        @JsonProperty("source_pdf") String sourcePdf,
        @JsonProperty("biomarker_count") Integer biomarkerCount,
        List<GoldenBiomarker> biomarkers,
        @JsonProperty("expected_findings") List<String> expectedFindings
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoldenBiomarker(
            String canonical,
            Double value,
            String unit,
            String reference,
            @JsonProperty("test_name") String testName
    ) {
    }
}
