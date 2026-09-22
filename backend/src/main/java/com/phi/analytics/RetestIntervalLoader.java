package com.phi.analytics;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
public class RetestIntervalLoader {

    private static final Logger log = LoggerFactory.getLogger(RetestIntervalLoader.class);

    private final RetestIntervalConfig config;

    public RetestIntervalLoader() {
        this.config = load();
        log.info("Loaded {} retest interval rules", config.rules().size());
    }

    public RetestIntervalConfig config() {
        return config;
    }

    private static RetestIntervalConfig load() {
        try (InputStream input = RetestIntervalLoader.class.getResourceAsStream("/rules/retest_intervals.yaml")) {
            if (input == null) {
                log.warn("retest_intervals.yaml not found — using empty config");
                return new RetestIntervalConfig(
                        new RetestIntervalConfig.General(12, "Routine health checkup", 30),
                        List.of()
                );
            }

            Yaml yaml = new Yaml();
            Object loaded = yaml.load(input);
            if (!(loaded instanceof Map<?, ?> root)) {
                return emptyConfig();
            }

            RetestIntervalConfig.General general = parseGeneral(root.get("general"));
            List<RetestIntervalConfig.Rule> rules = new ArrayList<>();
            Object rulesNode = root.get("rules");
            if (rulesNode instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        rules.add(parseRule(map));
                    }
                }
            }

            return new RetestIntervalConfig(general, rules);
        } catch (IOException exception) {
            log.warn("Failed to load retest_intervals.yaml: {}", exception.getMessage());
            return emptyConfig();
        }
    }

    private static RetestIntervalConfig emptyConfig() {
        return new RetestIntervalConfig(
                new RetestIntervalConfig.General(12, "Routine health checkup", 30),
                List.of()
        );
    }

    private static RetestIntervalConfig.General parseGeneral(Object node) {
        if (!(node instanceof Map<?, ?> map)) {
            return new RetestIntervalConfig.General(12, "Routine health checkup", 30);
        }
        int panelMonths = intValue(map.get("panel_months"), 12);
        String panelLabel = stringValue(map.get("panel_label"), "Routine health checkup");
        int dueSoonDays = intValue(map.get("due_soon_days"), 30);
        return new RetestIntervalConfig.General(panelMonths, panelLabel, dueSoonDays);
    }

    private static RetestIntervalConfig.Rule parseRule(Map<?, ?> map) {
        return new RetestIntervalConfig.Rule(
                stringValue(map.get("canonical"), ""),
                stringValue(map.get("display_name"), ""),
                intValue(map.get("abnormal_months"), 3),
                intValue(map.get("normal_months"), 12)
        );
    }

    private static String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? fallback : text;
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
