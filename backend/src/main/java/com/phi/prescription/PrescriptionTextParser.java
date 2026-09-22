package com.phi.prescription;

import com.phi.domain.MedicationCourseType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic parser for common Indian prescription layouts. Claude fills gaps when this returns empty.
 */
public final class PrescriptionTextParser {

    private static final Pattern MED_LINE = Pattern.compile(
            "(?i)(?:\\d+\\.?\\s*)?(?:tab(?:let)?|cap(?:sule)?|syp|syr|inj|drops?)\\.?\\s+(.+)"
    );
    private static final Pattern SCHEDULE = Pattern.compile(
            "(?i)(\\d-\\d-\\d|\\b(?:od|bd|tds|qid|hs|sos)\\b|(?:before|after)\\s+(?:food|meals))"
    );
    private static final Pattern DURATION = Pattern.compile(
            "(?i)(?:for\\s+)?(\\d+)\\s*(?:days?|d)\\b"
    );
    private static final List<String> CHRONIC_HINTS = List.of(
            "metformin", "glimepiride", "glipizide", "sitagliptin",
            "amlodipine", "telmisartan", "losartan", "atenolol", "metoprolol",
            "atorvastatin", "rosuvastatin", "levothyroxine", "thyroxine",
            "insulin", "warfarin", "aspirin"
    );

    private PrescriptionTextParser() {
    }

    public static List<PrescriptionDtos.ExtractedMedicationItem> parse(String text, LocalDate defaultStartDate) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<PrescriptionDtos.ExtractedMedicationItem> items = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.length() < 4) {
                continue;
            }
            Matcher matcher = MED_LINE.matcher(trimmed);
            if (!matcher.find()) {
                continue;
            }
            String body = matcher.group(1).trim();
            if (body.length() < 3) {
                continue;
            }

            String schedule = extractSchedule(trimmed);
            Integer durationDays = extractDuration(trimmed);
            String name = cleanMedicationName(body);
            MedicationCourseType courseType = inferCourseType(name, durationDays);

            items.add(new PrescriptionDtos.ExtractedMedicationItem(
                    name,
                    extractDosage(body),
                    schedule,
                    defaultStartDate,
                    durationDays,
                    courseType
            ));
        }
        return items;
    }

    private static String extractSchedule(String line) {
        Matcher matcher = SCHEDULE.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static Integer extractDuration(String line) {
        Matcher matcher = DURATION.matcher(line);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private static MedicationCourseType inferCourseType(String name, Integer durationDays) {
        if (durationDays != null && durationDays > 0) {
            return MedicationCourseType.ACUTE;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String hint : CHRONIC_HINTS) {
            if (lower.contains(hint)) {
                return MedicationCourseType.CHRONIC;
            }
        }
        return MedicationCourseType.UNKNOWN;
    }

    private static String cleanMedicationName(String body) {
        String cleaned = body.replaceAll("(?i)\\b(?:od|bd|tds|qid|hs|sos)\\b", "");
        cleaned = cleaned.replaceAll(DURATION.pattern(), "");
        cleaned = cleaned.replaceAll(SCHEDULE.pattern(), "");
        cleaned = cleaned.replaceAll("\\s{2,}", " ").trim();
        if (cleaned.length() > 120) {
            cleaned = cleaned.substring(0, 120).trim();
        }
        return cleaned;
    }

    private static String extractDosage(String body) {
        Matcher matcher = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?\\s*(?:mg|mcg|g|ml|iu|units?))").matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
