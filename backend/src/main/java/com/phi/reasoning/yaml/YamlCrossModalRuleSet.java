package com.phi.reasoning.yaml;

import java.util.List;

public record YamlCrossModalRuleSet(
        String id,
        String domain,
        String title,
        String severity,
        String message,
        List<String> imagingTextAny,
        List<String> biomarkerPresent
) {
}
