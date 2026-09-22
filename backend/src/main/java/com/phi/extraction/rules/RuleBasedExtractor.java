package com.phi.extraction.rules;

public interface RuleBasedExtractor {

    String labFormat();

    boolean supports(String extractedText);

    RuleExtractionResult extract(String extractedText);
}
