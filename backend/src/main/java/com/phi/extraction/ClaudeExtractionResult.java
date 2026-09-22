package com.phi.extraction;

import java.util.List;

public record ClaudeExtractionResult(List<ClaudeBiomarkerDto> biomarkers, ClaudeApiUsage usage) {
}
