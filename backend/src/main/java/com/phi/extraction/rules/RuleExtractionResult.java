package com.phi.extraction.rules;

import com.phi.extraction.ClaudeBiomarkerDto;
import java.util.List;

public record RuleExtractionResult(
        String labFormat,
        List<ClaudeBiomarkerDto> biomarkers,
        double coverage
) {
    public boolean meetsCoverage(double minimumCoverage) {
        return coverage >= minimumCoverage && !biomarkers.isEmpty();
    }
}
