package com.phi.reasoning.yaml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class BiomarkerExpressionEvaluatorTest {

    @Test
    void evaluatesSimpleComparison() {
        assertTrue(BiomarkerExpressionEvaluator.evaluate("lp_a > 30", Map.of("lp_a", 85.2)));
        assertFalse(BiomarkerExpressionEvaluator.evaluate("lp_a > 30", Map.of("lp_a", 20.0)));
    }

    @Test
    void evaluatesCompoundExpression() {
        Map<String, Double> values = Map.of("vitamin_d", 21.3);
        assertTrue(BiomarkerExpressionEvaluator.evaluate("vitamin_d >= 20 and vitamin_d < 30", values));
        assertFalse(BiomarkerExpressionEvaluator.evaluate("vitamin_d >= 30", values));
    }
}
