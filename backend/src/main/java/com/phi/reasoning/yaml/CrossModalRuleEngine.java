package com.phi.reasoning.yaml;

import com.phi.domain.ImagingModality;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CrossModalRuleEngine {

    private final CrossModalRuleLoader loader;

    public CrossModalRuleEngine(CrossModalRuleLoader loader) {
        this.loader = loader;
    }

    public List<CrossModalFinding> evaluate(CrossModalContext context) {
        List<CrossModalFinding> findings = new ArrayList<>();

        for (YamlCrossModalRuleSet rule : loader.rules()) {
            if (!matchesImaging(rule, context.imagingTexts())) {
                continue;
            }
            if (!matchesBiomarkers(rule, context.presentBiomarkers())) {
                continue;
            }
            findings.add(new CrossModalFinding(
                    rule.id(),
                    rule.title(),
                    renderMessage(rule.message(), context),
                    rule.severity() != null ? rule.severity() : "info"
            ));
        }

        return findings;
    }

    private static boolean matchesImaging(YamlCrossModalRuleSet rule, List<String> imagingTexts) {
        if (rule.imagingTextAny() == null || rule.imagingTextAny().isEmpty()) {
            return true;
        }
        if (imagingTexts.isEmpty()) {
            return false;
        }
        for (String text : imagingTexts) {
            for (String needle : rule.imagingTextAny()) {
                if (text.contains(needle.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchesBiomarkers(YamlCrossModalRuleSet rule, Set<String> presentBiomarkers) {
        if (rule.biomarkerPresent() == null || rule.biomarkerPresent().isEmpty()) {
            return true;
        }
        for (String canonical : rule.biomarkerPresent()) {
            if (presentBiomarkers.contains(canonical)) {
                return true;
            }
        }
        return false;
    }

    private static String renderMessage(String template, CrossModalContext context) {
        if (template == null) {
            return "";
        }
        return template
                .replace("{modality}", context.modalityLabel())
                .replace("{study_date}", context.studyDate() != null ? context.studyDate() : "")
                .replace("{impression_snippet}", truncate(context.impression(), 140));
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max).trim() + "…";
    }

    public record CrossModalContext(
            List<String> imagingTexts,
            Set<String> presentBiomarkers,
            ImagingModality modality,
            String studyDate,
            String impression
    ) {
        public String modalityLabel() {
            if (modality == null) {
                return "imaging study";
            }
            return switch (modality) {
                case ULTRASOUND -> "ultrasound";
                case XRAY -> "X-ray";
                case MRI -> "MRI";
                case CT -> "CT scan";
                default -> "imaging study";
            };
        }
    }

    public record CrossModalFinding(
            String ruleId,
            String title,
            String message,
            String severity
    ) {
    }
}
