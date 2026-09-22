package com.phi.golden;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoldenImagingFixture(
        String patient,
        @JsonProperty("report_date") String reportDate,
        String source,
        @JsonProperty("source_text") String sourceText,
        ExpectedStudy expected
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExpectedStudy(
            String modality,
            @JsonProperty("body_region") String bodyRegion,
            @JsonProperty("impression_contains") String impressionContains,
            @JsonProperty("facility_contains") String facilityContains,
            @JsonProperty("min_findings") Integer minFindings
    ) {
    }
}
