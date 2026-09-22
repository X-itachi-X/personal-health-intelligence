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
public class CrossModalRuleLoader {

    private static final Logger log = LoggerFactory.getLogger(CrossModalRuleLoader.class);

    private final List<YamlCrossModalRuleSet> cachedRules;

    public CrossModalRuleLoader() {
        this.cachedRules = loadAll();
        log.info("Loaded {} cross-modal YAML rule sets", cachedRules.size());
    }

    public List<YamlCrossModalRuleSet> rules() {
        return cachedRules;
    }

    private static List<YamlCrossModalRuleSet> loadAll() {
        List<YamlCrossModalRuleSet> loaded = new ArrayList<>();
        loaded.addAll(loadFromDirectory(Path.of("rules/cross_modal")));
        loaded.addAll(loadFromDirectory(Path.of("../rules/cross_modal")));
        loaded.addAll(loadFromClasspath());
        return loaded;
    }

    private static List<YamlCrossModalRuleSet> loadFromClasspath() {
        List<YamlCrossModalRuleSet> loaded = new ArrayList<>();
        try {
            var resourceUrl = CrossModalRuleLoader.class.getResource("/rules/cross_modal");
            if (resourceUrl != null) {
                Path dir = Path.of(resourceUrl.toURI());
                if (Files.isDirectory(dir)) {
                    try (Stream<Path> paths = Files.list(dir)) {
                        paths.filter(path -> path.toString().endsWith(".yaml"))
                                .forEach(path -> loadFile(path, loaded));
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Classpath cross-modal rules load skipped: {}", ex.getMessage());
        }
        return loaded;
    }

    private static List<YamlCrossModalRuleSet> loadFromDirectory(Path directory) {
        List<YamlCrossModalRuleSet> loaded = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return loaded;
        }
        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(path -> path.toString().endsWith(".yaml")).forEach(path -> loadFile(path, loaded));
        } catch (IOException ex) {
            log.debug("Could not read cross-modal rules directory {}: {}", directory, ex.getMessage());
        }
        return loaded;
    }

    private static void loadFile(Path path, List<YamlCrossModalRuleSet> loaded) {
        try (InputStream input = Files.newInputStream(path)) {
            YamlCrossModalRuleSet ruleSet = parse(input);
            if (loaded.stream().noneMatch(existing -> existing.id().equals(ruleSet.id()))) {
                loaded.add(ruleSet);
            }
        } catch (IOException ex) {
            log.warn("Failed to load cross-modal rule file {}: {}", path, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static YamlCrossModalRuleSet parse(InputStream input) {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(input);
        return new YamlCrossModalRuleSet(
                stringValue(root.get("id")),
                stringValue(root.get("domain")),
                stringValue(root.get("title")),
                stringValue(root.get("severity")),
                stringValue(root.get("message")),
                stringList(root.get("imaging_text_any")),
                stringList(root.get("biomarker_present"))
        );
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
