package com.phi.reasoning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.phi.analytics.AnalyticsDtos;
import com.phi.reasoning.TrendInsightAnalyzer.Direction;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrendInsightAnalyzerTest {

    @Test
    void detectsIncreasingTrend() {
        var points = List.of(
                point("2025-01-01", 90),
                point("2025-06-01", 102),
                point("2026-01-01", 118)
        );

        var insight = TrendInsightAnalyzer.analyze(points);
        assertEquals(Direction.INCREASING.name(), insight.direction());
    }

    @Test
    void detectsFluctuatingTrend() {
        var points = List.of(
                point("2025-01-01", 100),
                point("2025-06-01", 130),
                point("2026-01-01", 95),
                point("2026-06-01", 125)
        );

        var insight = TrendInsightAnalyzer.analyze(points);
        assertEquals(Direction.FLUCTUATING.name(), insight.direction());
    }

    private static AnalyticsDtos.TrendPoint point(String date, double value) {
        return new AnalyticsDtos.TrendPoint(date, value, "mg/dL", 1L);
    }
}
