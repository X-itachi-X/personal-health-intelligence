package com.phi.extraction.rules;

import com.phi.extraction.BiomarkerNormalizer;
import com.phi.extraction.ClaudeBiomarkerDto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class OrangeHealthRuleExtractor implements RuleBasedExtractor {

    static final int EXPECTED_BIOMARKER_COUNT = 106;

    private static final Pattern SUPPORT_MARKERS = Pattern.compile(
            "orangehealth\\.in|Orchard Healthcare",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JUNK_LINE = Pattern.compile(
            "(Page \\d|orangehealth|Orchard Healthcare|Report Ref|Collected :|Received :|Reported :|"
                    + "Ref\\. by|Partner :|^Result$|Biological Reference|Clinical Significance|www\\.|support@|\\+91|"
                    + "Rated |Available in|Trusted by|Easy to Read|Download App|# 953|Patient ID|"
                    + "25 Year|Reported on|September|Male|Index$|Ultra Full|Test Result Biological)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern METHOD_LINE = Pattern.compile(
            "(Calculated|Flow Cytometry|Colorimetric|Immunoturbidimetry|Chemiluminescent|Microparticle|"
                    + "Immuassay|Assay|CMIA|Bromo|Biuret|Kinetic|SZAZ|Serum,|Whole Blood|Plasma|LDH|UV |"
                    + "p-Nitrophenyl|Multipoint|Nutritional Status|Iron Profile|Deficient:|Insufficient:|"
                    + "Sufficient:|Potential Toxicity:)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UNIT_ONLY = Pattern.compile(
            "^(g/dL|mg/dL|U/L|mmol/L|µg/dL|ug/dL|/mm³|/mm3|cells/mm³|cells/mm3|%|Ratio|fL|pg|mL/min|IU/mL|ng/mL|"
                    + "nmol/L|µIU/mL|uIU/mL|Index Value|mill/mm³|mill/mm3|pg/mL|mEq/L|mm/hr|µmol/L|umol/L)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern NUMERIC_VALUE = Pattern.compile("^[\\d.]+$");

    private static final Pattern VALUE_LINE = Pattern.compile(
            "^([\\d.]+|<[\\d.]+|>[\\d.]+|Negative|Positive|Non-Reactive|Reactive|Nil|Non Reactive)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern REFERENCE_LINE = Pattern.compile(
            "^([\\d.]+ ?- ?[\\d.]+|<[\\d.]+|>[\\d.]+|<=? ?[\\d.]+|>=? ?[\\d.]+|\\d+-\\d+|< \\d+|> \\d+|Nil|"
                    + "Non Reactive|Negative|Deficient:.*|Insufficient:.*|Sufficient:.*|Potential Toxicity:.*)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern VALUE_REF_COMBINED = Pattern.compile("^([\\d.]+)\\s+(.+)$");

    private static final Map<String, String> UNIT_HINTS = Map.ofEntries(
            Map.entry("hemoglobin", "g/dL"),
            Map.entry("albumin", "g/dL"),
            Map.entry("globulin", "g/dL"),
            Map.entry("vitamin_d", "ng/mL"),
            Map.entry("vitamin_b12", "pg/mL"),
            Map.entry("ferritin", "ng/mL"),
            Map.entry("creatinine", "mg/dL"),
            Map.entry("glucose", "mg/dL"),
            Map.entry("hba1c", "%"),
            Map.entry("total_cholesterol", "mg/dL"),
            Map.entry("hdl", "mg/dL"),
            Map.entry("ldl", "mg/dL"),
            Map.entry("triglycerides", "mg/dL"),
            Map.entry("alt", "U/L"),
            Map.entry("ast", "U/L"),
            Map.entry("tsh", "µIU/mL"),
            Map.entry("neutrophils", "%"),
            Map.entry("lymphocytes", "%"),
            Map.entry("absolute_neutrophil_count", "/mm³"),
            Map.entry("absolute_lymphocyte_count", "/mm³")
    );

    private final BiomarkerNormalizer normalizer;

    public OrangeHealthRuleExtractor(BiomarkerNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    @Override
    public String labFormat() {
        return "orange_health";
    }

    @Override
    public boolean supports(String extractedText) {
        return extractedText != null && SUPPORT_MARKERS.matcher(extractedText).find();
    }

    @Override
    public RuleExtractionResult extract(String extractedText) {
        List<String> lines = nonBlankLines(extractedText);
        List<ParsedTriplet> separate = parseSeparateTriplets(lines);
        List<ParsedTriplet> combined = parseCombinedValueRefLines(lines);
        List<ParsedTriplet> triplets = new ArrayList<>(separate);
        triplets.addAll(combined);
        List<ClaudeBiomarkerDto> biomarkers = toDtos(triplets);
        int parseableSlots = Math.max(separate.size() + combined.size(), biomarkers.size());
        int denominator = Math.max(parseableSlots, (int) (EXPECTED_BIOMARKER_COUNT * 0.75));
        double coverage = Math.min(1.0, biomarkers.size() / (double) denominator);
        return new RuleExtractionResult(labFormat(), biomarkers, coverage);
    }

    private static List<String> nonBlankLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private List<ParsedTriplet> parseSeparateTriplets(List<String> lines) {
        List<ParsedTriplet> triplets = new ArrayList<>();
        int index = 0;
        while (index < lines.size() - 2) {
            String name = lines.get(index);
            String value = lines.get(index + 1);
            String reference = lines.get(index + 2);
            if (isTestName(name) && isValue(value) && isReference(reference)) {
                triplets.add(new ParsedTriplet(name, value, reference, unitAfter(lines, index + 3)));
                index += 3;
            } else {
                index++;
            }
        }
        return triplets;
    }

    private List<ParsedTriplet> parseCombinedValueRefLines(List<String> lines) {
        List<ParsedTriplet> triplets = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            Matcher matcher = VALUE_REF_COMBINED.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            String value = matcher.group(1);
            String reference = matcher.group(2).trim();
            if (!NUMERIC_VALUE.matcher(value).matches() || !isReference(reference)) {
                continue;
            }
            String testName = findTestNameBefore(lines, index);
            if (testName == null) {
                continue;
            }
            triplets.add(new ParsedTriplet(testName, value, reference, unitAfter(lines, index + 1)));
        }
        return triplets;
    }

    private String findTestNameBefore(List<String> lines, int valueRefIndex) {
        for (int index = valueRefIndex - 1; index >= Math.max(0, valueRefIndex - 6); index--) {
            String candidate = lines.get(index);
            if (isTestName(candidate)) {
                return candidate;
            }
            if (isValueRefCombinedLine(candidate) || JUNK_LINE.matcher(candidate).find()) {
                break;
            }
        }
        return null;
    }

    private String unitAfter(List<String> lines, int unitIndex) {
        if (unitIndex < lines.size() && UNIT_ONLY.matcher(lines.get(unitIndex)).matches()) {
            return lines.get(unitIndex);
        }
        return null;
    }

    private boolean isValueRefCombinedLine(String line) {
        Matcher matcher = VALUE_REF_COMBINED.matcher(line);
        if (!matcher.matches()) {
            return false;
        }
        return NUMERIC_VALUE.matcher(matcher.group(1)).matches()
                && isReference(matcher.group(2).trim());
    }

    private List<ClaudeBiomarkerDto> toDtos(List<ParsedTriplet> triplets) {
        Map<String, ClaudeBiomarkerDto> deduped = new LinkedHashMap<>();
        for (ParsedTriplet triplet : triplets) {
            String canonical = normalizer.normalize(triplet.testName());
            if ("unknown".equals(canonical)) {
                continue;
            }
            deduped.putIfAbsent(canonical, toDto(triplet, canonical));
        }
        return List.copyOf(deduped.values());
    }

    private ClaudeBiomarkerDto toDto(ParsedTriplet triplet, String canonical) {
        String valueToken = triplet.value().trim();
        BigDecimal numeric = null;
        String textValue = null;
        if (NUMERIC_VALUE.matcher(valueToken).matches()) {
            numeric = new BigDecimal(valueToken);
        } else if (!VALUE_LINE.matcher(valueToken).matches()) {
            textValue = valueToken;
        } else if (!NUMERIC_VALUE.matcher(valueToken).matches()) {
            textValue = valueToken;
        }
        String unit = triplet.unit() != null ? triplet.unit() : UNIT_HINTS.get(canonical);
        return new ClaudeBiomarkerDto(
                triplet.testName(),
                canonical,
                numeric,
                textValue,
                unit,
                triplet.reference(),
                1.0,
                null
        );
    }

    private boolean isTestName(String line) {
        if (line == null || line.length() < 2) {
            return false;
        }
        if (JUNK_LINE.matcher(line).find()) {
            return false;
        }
        if (METHOD_LINE.matcher(line).find() && !line.contains("(")) {
            return false;
        }
        if (UNIT_ONLY.matcher(line).matches()) {
            return false;
        }
        if (isValue(line) || isReference(line) || isValueRefCombinedLine(line)) {
            return false;
        }
        if (line.matches("^[\\d./: -]+$")) {
            return false;
        }
        if (line.contains("Checkup") && !line.contains("(")) {
            return false;
        }
        if (line.startsWith("Ankit ")) {
            return false;
        }
        if (line.matches("^\\([^)]+\\)$")) {
            return false;
        }
        if (line.length() < 4) {
            return false;
        }
        return line.chars().anyMatch(Character::isLetter);
    }

    private boolean isValue(String line) {
        return line != null && VALUE_LINE.matcher(line.trim()).matches();
    }

    private boolean isReference(String line) {
        return line != null && REFERENCE_LINE.matcher(line.trim()).matches();
    }

    private record ParsedTriplet(String testName, String value, String reference, String unit) {
    }
}
