package com.phi.golden;

import com.phi.domain.BiomarkerValue;
import com.phi.extraction.ClaudeBiomarkerDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class GoldenDatasetAssertions {

    private static final double VALUE_TOLERANCE = 0.05;

    private GoldenDatasetAssertions() {
    }

    public static void assertBiomarkersMatch(
            GoldenReportFixture fixture,
            List<BiomarkerValue> actualValues
    ) {
        Map<String, BiomarkerValue> actualByCanonical = actualValues.stream()
                .collect(Collectors.toMap(BiomarkerValue::getCanonicalName, Function.identity()));

        for (GoldenReportFixture.GoldenBiomarker expected : fixture.biomarkers()) {
            BiomarkerValue actual = actualByCanonical.get(expected.canonical());
            if (actual == null) {
                throw new AssertionError("Missing biomarker: " + expected.canonical());
            }
            assertNumericClose(expected.canonical(), expected.value(), actual.getNumericValue());
            if (expected.unit() != null && !expected.unit().isBlank()) {
                assertUnitMatches(expected.canonical(), expected.unit(), actual.getUnit());
            }
        }
    }

    public static List<ClaudeBiomarkerDto> toExtractionDtos(GoldenReportFixture fixture) {
        return fixture.biomarkers().stream()
                .map(b -> new ClaudeBiomarkerDto(
                        b.canonical(),
                        b.canonical(),
                        b.value() != null ? BigDecimal.valueOf(b.value()) : null,
                        null,
                        b.unit(),
                        b.reference(),
                        0.95,
                        1
                ))
                .toList();
    }

    private static void assertNumericClose(String canonical, Double expected, BigDecimal actual) {
        if (expected == null) {
            if (actual != null) {
                throw new AssertionError(canonical + ": expected null value but got " + actual);
            }
            return;
        }
        if (actual == null) {
            throw new AssertionError(canonical + ": expected " + expected + " but value was null");
        }
        double delta = Math.abs(expected - actual.doubleValue());
        if (delta > VALUE_TOLERANCE) {
            throw new AssertionError(
                    canonical + ": expected " + expected + " but got " + actual + " (tolerance " + VALUE_TOLERANCE + ")"
            );
        }
    }

    private static void assertUnitMatches(String canonical, String expectedUnit, String actualUnit) {
        String normalizedExpected = normalizeUnit(expectedUnit);
        String normalizedActual = normalizeUnit(actualUnit);
        if (!normalizedExpected.equals(normalizedActual)) {
            throw new AssertionError(
                    canonical + ": expected unit '" + expectedUnit + "' but got '" + actualUnit + "'"
            );
        }
    }

    private static String normalizeUnit(String unit) {
        if (unit == null) {
            return "";
        }
        return unit
                .replace("µ", "u")
                .replace("μ", "u")
                .trim()
                .toLowerCase();
    }
}
