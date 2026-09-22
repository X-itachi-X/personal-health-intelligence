package com.phi.reasoning;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReferenceRangeAssessor {

    private static final Pattern BETWEEN = Pattern.compile("^([\\d.]+)\\s*[-–]\\s*([\\d.]+)$");
    private static final Pattern LESS_THAN = Pattern.compile("^<?=\\s*([\\d.]+)$");
    private static final Pattern GREATER_THAN = Pattern.compile("^>?=\\s*([\\d.]+)$");

    public enum Status {
        NORMAL,
        LOW,
        HIGH,
        UNKNOWN
    }

    private ReferenceRangeAssessor() {
    }

    public static Status assess(BigDecimal value, String referenceRange) {
        if (value == null || referenceRange == null || referenceRange.isBlank()) {
            return Status.UNKNOWN;
        }

        Bounds bounds = parse(referenceRange);
        if (bounds == null) {
            return Status.UNKNOWN;
        }

        double numeric = value.doubleValue();
        if (bounds.min != null && numeric < bounds.min) {
            return Status.LOW;
        }
        if (bounds.max != null && numeric > bounds.max) {
            return Status.HIGH;
        }
        return Status.NORMAL;
    }

    private static Bounds parse(String referenceRange) {
        String cleaned = referenceRange.trim().replaceAll("\\s+", "");
        if (cleaned.isEmpty()) {
            return null;
        }

        Matcher between = BETWEEN.matcher(cleaned);
        if (between.matches()) {
            return new Bounds(Double.parseDouble(between.group(1)), Double.parseDouble(between.group(2)));
        }

        Matcher lessThan = LESS_THAN.matcher(cleaned);
        if (lessThan.matches()) {
            return new Bounds(null, Double.parseDouble(lessThan.group(1)));
        }

        Matcher greaterThan = GREATER_THAN.matcher(cleaned);
        if (greaterThan.matches()) {
            return new Bounds(Double.parseDouble(greaterThan.group(1)), null);
        }

        return null;
    }

    private record Bounds(Double min, Double max) {
    }
}
