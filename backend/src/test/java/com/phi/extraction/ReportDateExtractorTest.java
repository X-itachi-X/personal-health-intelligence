package com.phi.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReportDateExtractorTest {

    private final ReportDateExtractor extractor = new ReportDateExtractor();

    @Test
    void extractsOrangeHealthReportedOnDate() {
        String text = """
                Ankit Prakash                                      Reported on
                25 Year(s) Male                                    September 06, 2026
                """;

        assertEquals(LocalDate.of(2026, 9, 6), extractor.extract(text).orElseThrow());
    }

    @Test
    void extractsReportDateLabel() {
        String text = "Sample Collected on: 15/03/2024\nHemoglobin 14.2 g/dL";
        assertEquals(LocalDate.of(2024, 3, 15), extractor.extract(text).orElseThrow());
    }

    @Test
    void returnsEmptyWhenNoDateFound() {
        assertTrue(extractor.extract("Hemoglobin 14.2 g/dL").isEmpty());
    }
}
