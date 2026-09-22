package com.phi.reasoning.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.phi.domain.BiomarkerValue;
import com.phi.domain.LabReport;
import com.phi.domain.Person;
import com.phi.golden.GoldenDatasetLoader;
import com.phi.golden.GoldenReportFixture;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class YamlRuleEngineTest {

    private YamlRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new YamlRuleEngine(new YamlRuleLoader());
    }

    @Test
    void goldenReportTriggersExpectedClinicalRules() throws Exception {
        GoldenReportFixture fixture = new GoldenDatasetLoader().loadAll().stream()
                .map(GoldenDatasetLoader.LoadedFixture::fixture)
                .filter(item -> item.patient().equals("ankit"))
                .findFirst()
                .orElseThrow();
        LabReport report = labReport();
        List<BiomarkerValue> biomarkers = fixture.biomarkers().stream()
                .map(row -> BiomarkerValue.fromExtraction(
                        report,
                        row.canonical(),
                        row.testName(),
                        row.value() != null ? BigDecimal.valueOf(row.value()) : null,
                        null,
                        row.unit(),
                        row.reference(),
                        null,
                        null
                ))
                .toList();

        Set<String> ruleIds = engine.evaluate(biomarkers).stream()
                .map(YamlRuleEngine.ClinicalRuleFinding::ruleId)
                .collect(Collectors.toSet());

        for (String expected : fixture.expectedFindings()) {
            assertTrue(ruleIds.contains(expected), "Missing expected clinical rule: " + expected);
        }
        assertEquals(fixture.expectedFindings().size(), ruleIds.size());
    }

    private static LabReport labReport() {
        Person person = new Person("Test");
        return new LabReport(person, null, null, "test.pdf", "/tmp/test.pdf", "hash");
    }
}
