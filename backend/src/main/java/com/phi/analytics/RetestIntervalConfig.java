package com.phi.analytics;

import java.util.List;

public record RetestIntervalConfig(
        General general,
        List<Rule> rules
) {
    public record General(
            int panelMonths,
            String panelLabel,
            int dueSoonDays
    ) {
    }

    public record Rule(
            String canonical,
            String displayName,
            int abnormalMonths,
            int normalMonths
    ) {
    }
}
