package com.phi.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RetestReminderComputerTest {

    @Test
    void flagsOverdueAbnormalVitaminD() {
        RetestIntervalConfig config = new RetestIntervalConfig(
                new RetestIntervalConfig.General(12, "Routine health checkup", 30),
                List.of(new RetestIntervalConfig.Rule("vitamin_d", "Vitamin D", 3, 12))
        );

        Map<String, RetestReminderComputer.LatestBiomarkerReading> latest = Map.of(
                "vitamin_d",
                new RetestReminderComputer.LatestBiomarkerReading(
                        "vitamin_d",
                        BigDecimal.valueOf(15),
                        "30-100",
                        LocalDate.of(2025, 12, 1),
                        42L
                )
        );

        List<AnalyticsDtos.RetestReminder> reminders = RetestReminderComputer.compute(
                LocalDate.of(2026, 4, 1),
                config,
                latest,
                LocalDate.of(2025, 12, 1)
        );

        AnalyticsDtos.RetestReminder vitaminD = reminders.stream()
                .filter(reminder -> "biomarker-vitamin_d".equals(reminder.id()))
                .findFirst()
                .orElseThrow();
        assertEquals("OVERDUE", vitaminD.urgency());
        assertEquals("ABNORMAL_LAST", vitaminD.reason());
        assertEquals("2026-03-01", vitaminD.dueDate());
    }

    @Test
    void routinePanelDueSoonWhenWithinWindow() {
        RetestIntervalConfig config = new RetestIntervalConfig(
                new RetestIntervalConfig.General(12, "Routine health checkup", 30),
                List.of()
        );

        List<AnalyticsDtos.RetestReminder> reminders = RetestReminderComputer.compute(
                LocalDate.of(2026, 3, 15),
                config,
                Map.of(),
                LocalDate.of(2025, 4, 1)
        );

        AnalyticsDtos.RetestReminder panel = reminders.stream()
                .filter(reminder -> "general-panel".equals(reminder.id()))
                .findFirst()
                .orElseThrow();
        assertEquals("DUE_SOON", panel.urgency());
        assertTrue(panel.message().contains("Routine health checkup"));
    }
}
