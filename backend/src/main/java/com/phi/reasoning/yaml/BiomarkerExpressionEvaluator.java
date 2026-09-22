package com.phi.reasoning.yaml;

import java.util.Map;

/**
 * Evaluates simple biomarker expressions from YAML rules, e.g. {@code lp_a > 30}
 * and {@code vitamin_d >= 20 and vitamin_d < 30}.
 */
public final class BiomarkerExpressionEvaluator {

    private BiomarkerExpressionEvaluator() {
    }

    public static boolean evaluate(String expression, Map<String, Double> values) {
        if (expression == null || expression.isBlank()) {
            return false;
        }
        String trimmed = expression.trim();
        if (trimmed.contains(" and ")) {
            String[] parts = trimmed.split(" and ");
            for (String part : parts) {
                if (!evaluateSingle(part.trim(), values)) {
                    return false;
                }
            }
            return true;
        }
        return evaluateSingle(trimmed, values);
    }

    private static boolean evaluateSingle(String clause, Map<String, Double> values) {
        String[] operators = new String[] {">=", "<=", ">", "<", "=="};
        for (String operator : operators) {
            int index = clause.indexOf(operator);
            if (index <= 0) {
                continue;
            }
            String canonical = clause.substring(0, index).trim();
            String rawThreshold = clause.substring(index + operator.length()).trim();
            Double value = values.get(canonical);
            if (value == null) {
                return false;
            }
            double threshold = Double.parseDouble(rawThreshold);
            return switch (operator) {
                case ">=" -> value >= threshold;
                case "<=" -> value <= threshold;
                case ">" -> value > threshold;
                case "<" -> value < threshold;
                case "==" -> Double.compare(value, threshold) == 0;
                default -> false;
            };
        }
        throw new IllegalArgumentException("Unsupported expression clause: " + clause);
    }
}
