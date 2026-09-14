package com.phi.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClaudeBiomarkerDto(
        String testName,
        String canonical,
        BigDecimal value,
        String textValue,
        String unit,
        String referenceRange,
        Double confidence,
        Integer sourcePage
) {
}
