package com.phi.prescription;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PrescriptionTextParserTest {

    @Test
    void parsesCommonPrescriptionLines() {
        String text = """
                Rx
                1. Tab. Metformin 500 mg 1-0-1 after food for 30 days
                2. Cap. Amoxicillin 500 mg TDS for 5 days
                """;
        var items = PrescriptionTextParser.parse(text, LocalDate.of(2026, 9, 6));
        assertFalse(items.isEmpty());
        assertTrue(items.stream().anyMatch(item -> item.medicationName().toLowerCase().contains("metformin")));
        assertTrue(items.stream().anyMatch(item -> item.scheduleText() != null && item.scheduleText().contains("1-0-1")));
    }
}
