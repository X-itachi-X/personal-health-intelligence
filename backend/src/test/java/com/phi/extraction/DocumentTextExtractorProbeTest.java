package com.phi.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.phi.config.PhiProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentTextExtractorProbeTest {

    @TempDir
    Path tempDir;

    @Test
    void probeAllFreeTextToolsRunsPlainTextProbe() throws Exception {
        PhiProperties properties = testProperties();
        DocumentTextExtractor extractor = new DocumentTextExtractor(
                new PdfTextExtractor(),
                new OcrTextExtractor(properties, new TesseractCliRunner()),
                new TextQualityAssessor(properties)
        );

        Path textFile = tempDir.resolve("sample.txt");
        Files.writeString(textFile, "Hemoglobin 14.2 g/dL\nVitamin D 25 ng/mL");

        List<FreeTextProbe> probes = extractor.probeAllFreeTextTools(textFile);

        assertEquals(1, probes.size());
        assertEquals("text.plain", probes.get(0).toolId());
        assertEquals("success", probes.get(0).status());
        assertTrue(probes.get(0).text().contains("Hemoglobin"));
    }

    private static PhiProperties testProperties() {
        return new PhiProperties(
                new PhiProperties.Storage("/tmp", 0),
                new PhiProperties.Claude("", "", "", false, 0.8, false),
                new PhiProperties.Ocr(false, "eng", "", 150, 100, 80, 200, 3, 0),
                new PhiProperties.Jwt("test-secret-min-32-chars-long!!", 1),
                new PhiProperties.Google("test"),
                new PhiProperties.Analytics("/tmp/test.duckdb"),
                new PhiProperties.Platform("")
        );
    }
}
