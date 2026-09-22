package com.phi.extraction.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.phi.extraction.PdfTextExtractor;
import com.phi.golden.GoldenDatasetLoader;
import com.phi.golden.GoldenDatasetPaths;
import com.phi.golden.GoldenReportFixture;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OrangeHealthRuleExtractorTest {

    private static String orangeHealthText;
    private static GoldenReportFixture goldenFixture;

    @BeforeAll
    static void loadGoldenPdfText() throws Exception {
        GoldenDatasetLoader loader = new GoldenDatasetLoader();
        goldenFixture = loader.loadAll().getFirst().fixture();
        Path pdfPath = GoldenDatasetPaths.root()
                .resolve("ankit")
                .resolve(goldenFixture.sourcePdf());
        orangeHealthText = new PdfTextExtractor().extractText(pdfPath);
    }

    @Test
    void supportsOrangeHealthReportText() {
        OrangeHealthRuleExtractor extractor = new OrangeHealthRuleExtractor(new com.phi.extraction.BiomarkerNormalizer());
        assertTrue(extractor.supports(orangeHealthText));
    }

    @Test
    void extractsHighCoverageWithoutClaude() {
        OrangeHealthRuleExtractor extractor = new OrangeHealthRuleExtractor(new com.phi.extraction.BiomarkerNormalizer());
        RuleExtractionResult result = extractor.extract(orangeHealthText);

        assertEquals("orange_health", result.labFormat());
        assertTrue(
                result.biomarkers().size() >= 75,
                "expected at least 75 biomarkers, got " + result.biomarkers().size() + " coverage=" + result.coverage()
        );
        assertTrue(result.coverage() >= 0.8, "expected coverage >= 0.8, got " + result.coverage());
        assertTrue(result.meetsCoverage(0.8));
    }

    @Test
    void extractsKnownGoldenBiomarkers() {
        OrangeHealthRuleExtractor extractor = new OrangeHealthRuleExtractor(new com.phi.extraction.BiomarkerNormalizer());
        RuleExtractionResult result = extractor.extract(orangeHealthText);

        var albumin = result.biomarkers().stream()
                .filter(b -> "albumin".equals(b.canonical()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, albumin.value().compareTo(new java.math.BigDecimal("4.8")));

        var vitaminD = result.biomarkers().stream()
                .filter(b -> "vitamin_d".equals(b.canonical()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, vitaminD.value().compareTo(new java.math.BigDecimal("21.3")));
    }
}
