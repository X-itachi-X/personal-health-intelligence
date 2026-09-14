package com.phi.extraction;

import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BiomarkerNormalizer {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("vitamin d", "vitamin_d"),
            Map.entry("vitamin d 25-hydroxy", "vitamin_d"),
            Map.entry("25-oh vitamin d", "vitamin_d"),
            Map.entry("lipoprotein (a)", "lp_a"),
            Map.entry("lipoprotein(a)", "lp_a"),
            Map.entry("lp(a)", "lp_a"),
            Map.entry("alt", "alt"),
            Map.entry("sgpt", "alt"),
            Map.entry("hdl cholesterol", "hdl"),
            Map.entry("hdl", "hdl"),
            Map.entry("ldl cholesterol", "ldl"),
            Map.entry("ldl", "ldl"),
            Map.entry("hba1c", "hba1c"),
            Map.entry("hb a1c", "hba1c"),
            Map.entry("glycated hemoglobin", "hba1c"),
            Map.entry("rbc", "rbc"),
            Map.entry("red blood cell count", "rbc"),
            Map.entry("mcv", "mcv"),
            Map.entry("mch", "mch"),
            Map.entry("hemoglobin", "hemoglobin"),
            Map.entry("hb", "hemoglobin"),
            Map.entry("ferritin", "ferritin"),
            Map.entry("folate", "folate"),
            Map.entry("homocysteine", "homocysteine"),
            Map.entry("triglycerides", "triglycerides"),
            Map.entry("total cholesterol", "total_cholesterol"),
            Map.entry("tsh", "tsh"),
            Map.entry("creatinine", "creatinine"),
            Map.entry("egfr", "egfr")
    );

    public String normalize(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "unknown";
        }
        String key = rawName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9()\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return ALIASES.getOrDefault(key, key.replace(' ', '_'));
    }
}
