package com.phi.reasoning.yaml;

import com.phi.domain.BiomarkerValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class YamlRuleEngine {

    private final YamlRuleLoader loader;

    public YamlRuleEngine(YamlRuleLoader loader) {
        this.loader = loader;
    }

    public List<ClinicalRuleFinding> evaluate(List<BiomarkerValue> biomarkers) {
        Map<String, Double> values = toValueMap(biomarkers);
        List<ClinicalRuleFinding> findings = new ArrayList<>();

        for (YamlClinicalRuleSet ruleSet : loader.rules()) {
            if (!hasInputs(ruleSet, values)) {
                continue;
            }
            for (YamlClinicalRuleSet.YamlRuleCondition condition : ruleSet.rules()) {
                if (!BiomarkerExpressionEvaluator.evaluate(condition.when(), values)) {
                    continue;
                }
                findings.add(new ClinicalRuleFinding(
                        ruleSet.id(),
                        ruleSet.domain(),
                        primaryCanonical(ruleSet),
                        condition.severity(),
                        condition.action(),
                        condition.message()
                ));
                break;
            }
        }

        return findings;
    }

    private static boolean hasInputs(YamlClinicalRuleSet ruleSet, Map<String, Double> values) {
        if (ruleSet.inputs() == null || ruleSet.inputs().isEmpty()) {
            return true;
        }
        for (String input : ruleSet.inputs()) {
            if (!values.containsKey(input)) {
                return false;
            }
        }
        return true;
    }

    private static String primaryCanonical(YamlClinicalRuleSet ruleSet) {
        if (ruleSet.inputs() == null || ruleSet.inputs().isEmpty()) {
            return null;
        }
        return ruleSet.inputs().getFirst();
    }

    private static Map<String, Double> toValueMap(List<BiomarkerValue> biomarkers) {
        Map<String, Double> values = new HashMap<>();
        for (BiomarkerValue biomarker : biomarkers) {
            if (biomarker.getCanonicalName() == null || biomarker.getNumericValue() == null) {
                continue;
            }
            values.put(biomarker.getCanonicalName(), biomarker.getNumericValue().doubleValue());
        }
        return values;
    }

    public record ClinicalRuleFinding(
            String ruleId,
            String domain,
            String canonical,
            String severity,
            String action,
            String message
    ) {
    }
}
