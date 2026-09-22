package com.phi.analytics;

import com.phi.reasoning.ReferenceRangeAssessor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class RetestReminderComputer {

    private RetestReminderComputer() {
    }

    static List<AnalyticsDtos.RetestReminder> compute(
            LocalDate today,
            RetestIntervalConfig config,
            Map<String, LatestBiomarkerReading> latestByCanonical,
            LocalDate latestLabReportDate
    ) {
        List<AnalyticsDtos.RetestReminder> reminders = new ArrayList<>();

        for (RetestIntervalConfig.Rule rule : config.rules()) {
            if (rule.canonical().isBlank()) {
                continue;
            }
            LatestBiomarkerReading reading = latestByCanonical.get(rule.canonical());
            if (reading == null || reading.reportDate() == null) {
                continue;
            }

            ReferenceRangeAssessor.Status status = ReferenceRangeAssessor.assess(
                    reading.numericValue(),
                    reading.referenceRange()
            );
            boolean abnormal = status == ReferenceRangeAssessor.Status.HIGH
                    || status == ReferenceRangeAssessor.Status.LOW;
            int intervalMonths = abnormal ? rule.abnormalMonths() : rule.normalMonths();
            LocalDate dueDate = reading.reportDate().plusMonths(intervalMonths);
            String displayName = rule.displayName().isBlank()
                    ? formatCanonical(rule.canonical())
                    : rule.displayName();

            reminders.add(buildReminder(
                    today,
                    config.general().dueSoonDays(),
                    "biomarker-" + rule.canonical(),
                    displayName,
                    rule.canonical(),
                    reading.reportDate(),
                    dueDate,
                    abnormal,
                    reading.reportId()
            ));
        }

        if (latestLabReportDate != null) {
            LocalDate panelDue = latestLabReportDate.plusMonths(config.general().panelMonths());
            reminders.add(buildReminder(
                    today,
                    config.general().dueSoonDays(),
                    "general-panel",
                    config.general().panelLabel(),
                    null,
                    latestLabReportDate,
                    panelDue,
                    false,
                    null
            ));
        }

        reminders.sort(Comparator
                .comparingInt((AnalyticsDtos.RetestReminder reminder) -> urgencyRank(reminder.urgency()))
                .thenComparing(AnalyticsDtos.RetestReminder::dueDate));

        return reminders;
    }

    private static AnalyticsDtos.RetestReminder buildReminder(
            LocalDate today,
            int dueSoonDays,
            String id,
            String label,
            String canonical,
            LocalDate lastTestDate,
            LocalDate dueDate,
            boolean abnormal,
            Long reportId
    ) {
        String urgency = classifyUrgency(today, dueDate, dueSoonDays);
        String message = buildMessage(label, lastTestDate, dueDate, urgency, abnormal);
        return new AnalyticsDtos.RetestReminder(
                id,
                label,
                canonical,
                lastTestDate.toString(),
                dueDate.toString(),
                urgency,
                abnormal ? "ABNORMAL_LAST" : "ROUTINE",
                message,
                reportId
        );
    }

    private static String classifyUrgency(LocalDate today, LocalDate dueDate, int dueSoonDays) {
        if (!dueDate.isAfter(today)) {
            return "OVERDUE";
        }
        long daysUntil = ChronoUnit.DAYS.between(today, dueDate);
        if (daysUntil <= dueSoonDays) {
            return "DUE_SOON";
        }
        return "UPCOMING";
    }

    private static int urgencyRank(String urgency) {
        return switch (urgency) {
            case "OVERDUE" -> 0;
            case "DUE_SOON" -> 1;
            default -> 2;
        };
    }

    private static String buildMessage(
            String label,
            LocalDate lastTestDate,
            LocalDate dueDate,
            String urgency,
            boolean abnormal
    ) {
        String context = abnormal
                ? "Last result was outside the reference range."
                : "Routine monitoring interval.";
        return switch (urgency) {
            case "OVERDUE" -> String.format(
                    "%s follow-up is overdue (last tested %s). %s Discuss retesting with your doctor.",
                    label,
                    lastTestDate,
                    context
            );
            case "DUE_SOON" -> String.format(
                    "%s follow-up due by %s (last tested %s). %s",
                    label,
                    dueDate,
                    lastTestDate,
                    context
            );
            default -> String.format(
                    "%s recheck around %s (last tested %s). %s",
                    label,
                    dueDate,
                    lastTestDate,
                    context
            );
        };
    }

    private static String formatCanonical(String canonical) {
        return canonical.replace('_', ' ');
    }

    record LatestBiomarkerReading(
            String canonical,
            BigDecimal numericValue,
            String referenceRange,
            LocalDate reportDate,
            Long reportId
    ) {
    }
}
