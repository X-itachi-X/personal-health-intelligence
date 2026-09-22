package com.phi.telemetry;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TelemetryMetadataTest {

    @Test
    void textPreviewFitsLargeMultilineLabReports() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            text.append("Page ").append(i).append(" of 27\n");
            text.append("Rated 4.9/5 on Google\n");
            text.append("Hemoglobin 14.2 g/dL Reference 13.0-17.0\n");
        }

        String metadata = TelemetryMetadata.withTextPreview(text.toString());

        assertTrue(metadata.length() > 2048, "preview should exceed legacy VARCHAR(2048) limit");
        assertTrue(metadata.contains("\\n"), "newlines should be JSON-escaped");
        assertTrue(metadata.startsWith("{\"preview\":\""));
        assertTrue(metadata.endsWith("\"}"));
    }
}
