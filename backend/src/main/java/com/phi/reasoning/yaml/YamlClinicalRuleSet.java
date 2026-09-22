package com.phi.reasoning.yaml;

import java.util.List;

public record YamlClinicalRuleSet(
        String id,
        String domain,
        List<String> inputs,
        List<YamlRuleCondition> rules
) {
    public record YamlRuleCondition(
            String when,
            String severity,
            String action,
            String message
    ) {
    }
}
