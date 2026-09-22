package com.phi.extraction.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.phi.config.PhiProperties;
import com.phi.extraction.PdfTextExtractor;
import com.phi.golden.GoldenDatasetLoader;
import com.phi.golden.GoldenDatasetPaths;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RuleExtractionServiceTest {

    private static String orangeHealthText;
    private static RuleExtractionService service;

    @BeforeAll
    static void setUp() throws Exception {
        var fixture = new GoldenDatasetLoader().loadAll().getFirst().fixture();
        var pdfPath = GoldenDatasetPaths.root().resolve("ankit").resolve(fixture.sourcePdf());
        orangeHealthText = new PdfTextExtractor().extractText(pdfPath);

        PhiProperties properties = new PhiProperties(
                new PhiProperties.Storage("/tmp", 0),
                new PhiProperties.Claude("", "", "", true, 0.8, false),
                new PhiProperties.Ocr(false, "eng", "", 150, 100, 80, 200, 3, 0),
                new PhiProperties.Jwt("test-secret-min-32-characters-long!!", 1),
                new PhiProperties.Google(""),
                new PhiProperties.Analytics("/tmp/a.duckdb"),
                new PhiProperties.Platform("")
        );
        service = new RuleExtractionService(
                properties,
                List.of(new OrangeHealthRuleExtractor(new com.phi.extraction.BiomarkerNormalizer()))
        );
    }

    @Test
    void returnsResultWhenCoverageMeetsThreshold() {
        assertTrue(service.tryExtractIfSufficient(orangeHealthText).isPresent());
    }

    @Test
    void returnsEmptyWhenThresholdTooHigh() {
        PhiProperties strict = new PhiProperties(
                new PhiProperties.Storage("/tmp", 0),
                new PhiProperties.Claude("", "", "", true, 0.99, false),
                new PhiProperties.Ocr(false, "eng", "", 150, 100, 80, 200, 3, 0),
                new PhiProperties.Jwt("test-secret-min-32-characters-long!!", 1),
                new PhiProperties.Google(""),
                new PhiProperties.Analytics("/tmp/a.duckdb"),
                new PhiProperties.Platform("")
        );
        RuleExtractionService strictService = new RuleExtractionService(
                strict,
                List.of(new OrangeHealthRuleExtractor(new com.phi.extraction.BiomarkerNormalizer()))
        );
        assertFalse(strictService.tryExtractIfSufficient(orangeHealthText).isPresent());
    }
}
