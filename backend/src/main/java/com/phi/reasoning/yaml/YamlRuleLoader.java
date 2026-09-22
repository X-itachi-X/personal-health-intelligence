package com.phi.reasoning.yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
public class YamlRuleLoader {

    private static final Logger log = LoggerFactory.getLogger(YamlRuleLoader.class);

    private final List<YamlClinicalRuleSet> cachedRules;

    public YamlRuleLoader() {
        this.cachedRules = loadAll();
        log.info("Loaded {} clinical YAML rule sets", cachedRules.size());
    }

    public List<YamlClinicalRuleSet> rules() {
        return cachedRules;
    }

    private static List<YamlClinicalRuleSet> loadAll() {
        List<YamlClinicalRuleSet> loaded = new ArrayList<>();
        loaded.addAll(loadFromDirectory(Path.of("rules")));
        loaded.addAll(loadFromDirectory(Path.of("../rules")));
        loaded.addAll(loadFromClasspath());
        if (loaded.isEmpty()) {
            log.warn("No clinical YAML rules found under rules/ or classpath:/rules/");
        }
        return loaded;
    }

    private static List<YamlClinicalRuleSet> loadFromClasspath() {
        List<YamlClinicalRuleSet> loaded = new ArrayList<>();
        try (InputStream cardiovascular = YamlRuleLoader.class.getResourceAsStream("/rules/cardiovascular.yaml")) {
            if (cardiovascular != null) {
                loaded.add(parse(cardiovascular));
            }
            var url = YamlRuleLoader.class.getResource("/rules");
            if (url != null) {
                Path dir = Path.of(url.toURI());
                if (Files.isDirectory(dir)) {
                    try (Stream<Path> paths = Files.list(dir)) {
                        paths.filter(path -> path.toString().endsWith(".yaml"))
                                .filter(path -> !path.getFileName().toString().equals("cardiovascular.yaml"))
                                .forEach(path -> loadFile(path, loaded));
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Classpath rules load skipped: {}", ex.getMessage());
        }
        return loaded;
    }

    private static List<YamlClinicalRuleSet> loadFromDirectory(Path directory) {
        List<YamlClinicalRuleSet> loaded = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return loaded;
        }
        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(path -> path.toString().endsWith(".yaml")).forEach(path -> loadFile(path, loaded));
        } catch (IOException ex) {
            log.debug("Could not read rules directory {}: {}", directory, ex.getMessage());
        }
        return loaded;
    }

    private static void loadFile(Path path, List<YamlClinicalRuleSet> loaded) {
        try (InputStream input = Files.newInputStream(path)) {
            YamlClinicalRuleSet ruleSet = parse(input);
            if (loaded.stream().noneMatch(existing -> existing.id().equals(ruleSet.id()))) {
                loaded.add(ruleSet);
            }
        } catch (IOException ex) {
            log.warn("Failed to load rule file {}: {}", path, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static YamlClinicalRuleSet parse(InputStream input) {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(input);
        String id = stringValue(root.get("id"));
        String domain = stringValue(root.get("domain"));
        List<String> inputs = stringList(root.get("inputs"));
        List<Map<String, Object>> ruleMaps = (List<Map<String, Object>>) root.getOrDefault("rules", Collections.emptyList());
        List<YamlClinicalRuleSet.YamlRuleCondition> conditions = ruleMaps.stream()
                .map(rule -> new YamlClinicalRuleSet.YamlRuleCondition(
                        stringValue(rule.get("when")),
                        stringValue(rule.get("severity")),
                        stringValue(rule.get("action")),
                        stringValue(rule.get("message"))
                ))
                .toList();
        return new YamlClinicalRuleSet(id, domain, inputs, conditions);
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
