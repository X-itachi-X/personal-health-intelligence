package com.phi.extraction.rules;

import com.phi.config.PhiProperties;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RuleExtractionService {

    private static final Logger log = LoggerFactory.getLogger(RuleExtractionService.class);

    private final PhiProperties properties;
    private final List<RuleBasedExtractor> extractors;

    public RuleExtractionService(PhiProperties properties, List<RuleBasedExtractor> extractors) {
        this.properties = properties;
        this.extractors = extractors;
    }

    public Optional<RuleExtractionResult> tryExtract(String extractedText) {
        return extractors.stream()
                .filter(extractor -> extractor.supports(extractedText))
                .map(extractor -> extractor.extract(extractedText))
                .max(Comparator.comparingDouble(RuleExtractionResult::coverage));
    }

    public Optional<RuleExtractionResult> tryExtractIfSufficient(String extractedText) {
        double minimum = properties.claude().effectiveMinCoverage();
        Optional<RuleExtractionResult> result = tryExtract(extractedText);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        RuleExtractionResult extraction = result.get();
        if (!extraction.meetsCoverage(minimum)) {
            log.info(
                    "Rule extraction for {} found {} biomarkers (coverage {:.0f}% < {:.0f}%) — Claude fallback",
                    extraction.labFormat(),
                    extraction.biomarkers().size(),
                    extraction.coverage() * 100,
                    minimum * 100
            );
            return Optional.empty();
        }
        log.info(
                "Rule extraction for {} found {} biomarkers (coverage {:.0f}%) — skipping Claude",
                extraction.labFormat(),
                extraction.biomarkers().size(),
                extraction.coverage() * 100
        );
        return result;
    }
}
