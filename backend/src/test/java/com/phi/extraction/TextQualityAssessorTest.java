package com.phi.extraction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.phi.config.PhiProperties;
import org.junit.jupiter.api.Test;

class TextQualityAssessorTest {

    private final TextQualityAssessor assessor = new TextQualityAssessor(new PhiProperties(
            new PhiProperties.Storage("/tmp", 0),
                new PhiProperties.Claude("", "", "", true, 0.8, false),
            new PhiProperties.Ocr(true, "eng", "", 150, 100, 80, 300, 3, 0),
            new PhiProperties.Jwt("test-secret-min-32-characters-long!!", 1),
            new PhiProperties.Google(""),
                new PhiProperties.Analytics("/tmp/a.duckdb"),
                new PhiProperties.Platform("")
    ));

    @Test
    void rejectsEmptyPdfTextLayer() {
        assertFalse(assessor.isUsableTextLayer(""));
        assertFalse(assessor.isUsableTextLayer("short"));
    }

    @Test
    void acceptsRichPdfTextLayer() {
        String text = "Hemoglobin 14.2 g/dL ".repeat(20);
        assertTrue(assessor.isUsableTextLayer(text));
    }

    @Test
    void flagsVeryShortOcrOutput() {
        assertTrue(assessor.looksLowQualityOcr("abc 12"));
    }
}
