package com.phi.reasoning.yaml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.phi.domain.ImagingModality;
import com.phi.reasoning.yaml.CrossModalRuleEngine.CrossModalContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CrossModalRuleEngineTest {

    private CrossModalRuleEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CrossModalRuleEngine(new CrossModalRuleLoader());
    }

    @Test
    void fattyLiverRuleRequiresImagingAndAlt() {
        CrossModalContext withBoth = new CrossModalContext(
                List.of("grade i fatty liver noted"),
                Set.of("alt", "ast"),
                ImagingModality.ULTRASOUND,
                "2026-03-15",
                "Grade I fatty liver"
        );
        assertTrue(engine.evaluate(withBoth).stream().anyMatch(f -> f.ruleId().equals("fatty_liver_alt")));

        CrossModalContext missingLab = new CrossModalContext(
                List.of("grade i fatty liver noted"),
                Set.of("hemoglobin"),
                ImagingModality.ULTRASOUND,
                "2026-03-15",
                "Grade I fatty liver"
        );
        assertFalse(engine.evaluate(missingLab).stream().anyMatch(f -> f.ruleId().equals("fatty_liver_alt")));
    }

    @Test
    void chestRuleDoesNotRequireBiomarker() {
        CrossModalContext context = new CrossModalContext(
                List.of("mild pulmonary congestion"),
                Set.of(),
                ImagingModality.XRAY,
                "2026-01-10",
                "Mild pulmonary congestion. No effusion."
        );
        assertTrue(engine.evaluate(context).stream().anyMatch(f -> f.ruleId().equals("chest_imaging_context")));
    }
}
