package com.phi.extraction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Extracts the lab report date from extracted document text.
 * Used before biomarker parsing so trends use the real collection/report date.
 */
@Component
public class ReportDateExtractor {

    private static final Pattern REPORTED_ON_LINE = Pattern.compile(
            "Reported\\s+on\\s+([A-Za-z]+\\s+\\d{1,2},\\s+\\d{4})",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern REPORTED_ON_SPLIT = Pattern.compile(
            "Reported\\s+on[\\s\\S]{0,120}?([A-Za-z]+\\s+\\d{1,2},\\s+\\d{4})",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern REPORT_DATE_LABEL = Pattern.compile(
            "(?:Report\\s+Date|Date\\s+of\\s+Report|Sample\\s+Collected\\s+on|Collected\\s+on|Collection\\s+Date)"
                    + "\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}|[A-Za-z]+\\s+\\d{1,2},\\s+\\d{4})",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MONTH_NAME_DATE = Pattern.compile(
            "\\b([A-Za-z]{3,9}\\s+\\d{1,2},\\s+\\d{4})\\b"
    );

    private static final Pattern DMY_SLASH = Pattern.compile(
            "\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})\\b"
    );

    private static final Pattern YMD_SLASH = Pattern.compile(
            "\\b(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})\\b"
    );

    private static final DateTimeFormatter MONTH_DAY_YEAR = DateTimeFormatter.ofPattern(
            "MMMM d, yyyy",
            Locale.ENGLISH
    );

    public Optional<LocalDate> extract(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Optional<LocalDate> fromReportedOn = matchFirst(text, REPORTED_ON_LINE);
        if (fromReportedOn.isPresent()) {
            return fromReportedOn;
        }

        Optional<LocalDate> fromSplit = matchFirst(text, REPORTED_ON_SPLIT);
        if (fromSplit.isPresent()) {
            return fromSplit;
        }

        Optional<LocalDate> fromLabel = matchFirst(text, REPORT_DATE_LABEL);
        if (fromLabel.isPresent()) {
            return fromLabel;
        }

        Matcher monthMatcher = MONTH_NAME_DATE.matcher(text);
        while (monthMatcher.find()) {
            Optional<LocalDate> parsed = parseMonthNameDate(monthMatcher.group(1));
            if (parsed.isPresent() && isReasonable(parsed.get())) {
                return parsed;
            }
        }

        Matcher dmyMatcher = DMY_SLASH.matcher(text);
        while (dmyMatcher.find()) {
            Optional<LocalDate> parsed = parseDmy(
                    dmyMatcher.group(1),
                    dmyMatcher.group(2),
                    dmyMatcher.group(3)
            );
            if (parsed.isPresent() && isReasonable(parsed.get())) {
                return parsed;
            }
        }

        Matcher ymdMatcher = YMD_SLASH.matcher(text);
        while (ymdMatcher.find()) {
            Optional<LocalDate> parsed = parseYmd(
                    ymdMatcher.group(1),
                    ymdMatcher.group(2),
                    ymdMatcher.group(3)
            );
            if (parsed.isPresent() && isReasonable(parsed.get())) {
                return parsed;
            }
        }

        return Optional.empty();
    }

    private static Optional<LocalDate> matchFirst(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return parseFlexible(matcher.group(1));
    }

    private static Optional<LocalDate> parseFlexible(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String value = raw.trim();

        Optional<LocalDate> monthName = parseMonthNameDate(value);
        if (monthName.isPresent()) {
            return monthName;
        }

        Matcher dmy = DMY_SLASH.matcher(value);
        if (dmy.matches()) {
            return parseDmy(dmy.group(1), dmy.group(2), dmy.group(3));
        }

        Matcher ymd = YMD_SLASH.matcher(value);
        if (ymd.matches()) {
            return parseYmd(ymd.group(1), ymd.group(2), ymd.group(3));
        }

        return Optional.empty();
    }

    private static Optional<LocalDate> parseMonthNameDate(String value) {
        try {
            return Optional.of(LocalDate.parse(value.trim(), MONTH_DAY_YEAR));
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<LocalDate> parseDmy(String day, String month, String year) {
        try {
            int d = Integer.parseInt(day);
            int m = Integer.parseInt(month);
            int y = normalizeYear(Integer.parseInt(year));
            return Optional.of(LocalDate.of(y, m, d));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static Optional<LocalDate> parseYmd(String year, String month, String day) {
        try {
            int y = Integer.parseInt(year);
            int m = Integer.parseInt(month);
            int d = Integer.parseInt(day);
            return Optional.of(LocalDate.of(y, m, d));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static int normalizeYear(int year) {
        if (year < 100) {
            return year >= 70 ? 1900 + year : 2000 + year;
        }
        return year;
    }

    private static boolean isReasonable(LocalDate date) {
        LocalDate now = LocalDate.now();
        return !date.isAfter(now.plusDays(1)) && !date.isBefore(now.minusYears(30));
    }
}
