package com.phi.imaging;

import com.phi.domain.ImagingModality;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic parser for radiology report text (USG, X-ray, CT, MRI impressions).
 */
public final class ImagingTextParser {

    private static final Pattern IMPRESSION = Pattern.compile(
            "(?is)(?:impression|conclusion|summary)\\s*[:\\-]\\s*(.+?)(?:\\n\\s*\\n|$)"
    );
    private static final Pattern FINDINGS = Pattern.compile(
            "(?is)(?:findings|observations)\\s*[:\\-]\\s*(.+?)(?:\\n\\s*impression|\\n\\s*conclusion|$)"
    );
    private static final Pattern BULLET = Pattern.compile("(?m)^\\s*(?:[-•*]|\\d+\\.)\\s+(.+)$");

    private ImagingTextParser() {
    }

    public static ImagingDtos.ExtractedImagingStudy parse(String text, LocalDate defaultStudyDate) {
        if (text == null || text.isBlank()) {
            return emptyStudy(defaultStudyDate);
        }

        String normalized = text.replace('\r', '\n');
        ImagingModality modality = detectModality(normalized);
        String bodyRegion = detectBodyRegion(normalized);
        String impression = extractSection(IMPRESSION, normalized);
        String findingsBlock = extractSection(FINDINGS, normalized);
        List<ImagingDtos.ExtractedFinding> findings = parseFindings(findingsBlock, impression);

        if (impression == null && !findings.isEmpty()) {
            impression = findings.getFirst().findingText();
        }

        return new ImagingDtos.ExtractedImagingStudy(
                modality.name(),
                bodyRegion,
                defaultStudyDate,
                extractFacility(normalized),
                impression,
                findings
        );
    }

    private static ImagingDtos.ExtractedImagingStudy emptyStudy(LocalDate defaultStudyDate) {
        return new ImagingDtos.ExtractedImagingStudy(
                ImagingModality.OTHER.name(),
                null,
                defaultStudyDate,
                null,
                null,
                List.of()
        );
    }

    private static ImagingModality detectModality(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("mri") || lower.contains("magnetic resonance")) {
            return ImagingModality.MRI;
        }
        if (lower.contains("ultrasound") || lower.contains("sonography") || lower.contains(" usg ")
                || lower.matches("(?s).*\\busg\\b.*")) {
            return ImagingModality.ULTRASOUND;
        }
        if (lower.contains("ct scan") || lower.contains("computed tomography") || lower.contains(" hrct ")) {
            return ImagingModality.CT;
        }
        if (lower.contains("x-ray") || lower.contains("x ray") || lower.contains("xray") || lower.contains("radiograph")) {
            return ImagingModality.XRAY;
        }
        return ImagingModality.OTHER;
    }

    private static String detectBodyRegion(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        String[] regions = {
                "abdomen", "chest", "brain", "head", "knee", "spine", "pelvis", "thyroid",
                "liver", "kidney", "renal", "cardiac", "heart", "breast", "neck"
        };
        for (String region : regions) {
            if (lower.contains(region)) {
                return region;
            }
        }
        return null;
    }

    private static String extractFacility(String text) {
        Matcher matcher = Pattern.compile("(?im)^(?:hospital|clinic|centre|center|diagnostic)\\s*[:\\-]?\\s*(.+)$")
                .matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private static String extractSection(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1).trim();
        return value.isBlank() ? null : value.replaceAll("\\s{2,}", " ");
    }

    private static List<ImagingDtos.ExtractedFinding> parseFindings(String findingsBlock, String impression) {
        List<ImagingDtos.ExtractedFinding> findings = new ArrayList<>();
        if (findingsBlock != null) {
            appendBullets(findings, findingsBlock);
        }
        if (findings.isEmpty() && impression != null) {
            for (String sentence : impression.split("(?<=[.;])\\s+")) {
                String trimmed = sentence.trim();
                if (trimmed.length() >= 12) {
                    findings.add(new ImagingDtos.ExtractedFinding(trimmed, inferSeverity(trimmed), null, null));
                }
            }
        }
        return findings.stream().limit(12).toList();
    }

    private static void appendBullets(List<ImagingDtos.ExtractedFinding> findings, String block) {
        Matcher matcher = BULLET.matcher(block);
        boolean matched = false;
        while (matcher.find()) {
            matched = true;
            String line = matcher.group(1).trim();
            if (!line.isBlank()) {
                findings.add(new ImagingDtos.ExtractedFinding(line, inferSeverity(line), null, null));
            }
        }
        if (!matched) {
            for (String line : block.split("\\n")) {
                String trimmed = line.trim();
                if (trimmed.length() >= 12) {
                    findings.add(new ImagingDtos.ExtractedFinding(trimmed, inferSeverity(trimmed), null, null));
                }
            }
        }
    }

    private static String inferSeverity(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("severe") || lower.contains("mass") || lower.contains("malignan")) {
            return "high";
        }
        if (lower.contains("moderate") || lower.contains("enlarged") || lower.contains("stone")) {
            return "medium";
        }
        if (lower.contains("mild") || lower.contains("grade i") || lower.contains("minimal")) {
            return "low";
        }
        return "info";
    }
}
