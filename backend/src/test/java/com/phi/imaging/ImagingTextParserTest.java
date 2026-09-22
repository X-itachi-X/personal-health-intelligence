package com.phi.imaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ImagingTextParserTest {

    @Test
    void parsesUltrasoundImpression() {
        String text = """
                ULTRASOUND ABDOMEN
                Findings:
                - Liver is normal in size with increased echogenicity suggestive of grade I fatty liver.
                - Kidneys are normal.
                Impression: Grade I fatty liver. Otherwise unremarkable study.
                """;
        ImagingDtos.ExtractedImagingStudy study = ImagingTextParser.parse(text, LocalDate.of(2026, 3, 15));
        assertEquals("ULTRASOUND", study.modality());
        assertEquals("abdomen", study.bodyRegion());
        assertTrue(study.impression() != null && study.impression().toLowerCase().contains("fatty liver"));
        assertFalse(study.findings().isEmpty());
    }
}
