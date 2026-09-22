package com.phi.reasoning;

import com.phi.analytics.AnalyticsDtos;
import java.util.List;

public final class TrendInsightAnalyzer {

    public enum Direction {
        INSUFFICIENT_DATA,
        STABLE,
        INCREASING,
        DECREASING,
        FLUCTUATING
    }

    private TrendInsightAnalyzer() {
    }

    public static AnalyticsDtos.TrendInsight analyze(List<AnalyticsDtos.TrendPoint> points) {
        if (points == null || points.isEmpty()) {
            return new AnalyticsDtos.TrendInsight(
                    Direction.INSUFFICIENT_DATA.name(),
                    "Not enough lab reports yet.",
                    0,
                    null,
                    null,
                    null,
                    null
            );
        }

        if (points.size() == 1) {
            AnalyticsDtos.TrendPoint only = points.getFirst();
            return new AnalyticsDtos.TrendInsight(
                    Direction.INSUFFICIENT_DATA.name(),
                    "One reading on file — upload another report to see a trend.",
                    1,
                    only.value(),
                    null,
                    null,
                    null
            );
        }

        AnalyticsDtos.TrendPoint latest = points.getLast();
        AnalyticsDtos.TrendPoint previous = points.get(points.size() - 2);
        Double delta = latest.value() - previous.value();
        Double percentChange = previous.value() != 0
                ? (delta / previous.value()) * 100.0
                : null;

        Direction direction = classifyDirection(points);
        String summary = summarize(direction, latest.value(), delta, percentChange);

        return new AnalyticsDtos.TrendInsight(
                direction.name(),
                summary,
                points.size(),
                latest.value(),
                previous.value(),
                delta,
                percentChange
        );
    }

    private static Direction classifyDirection(List<AnalyticsDtos.TrendPoint> points) {
        if (points.size() < 3) {
            double delta = points.getLast().value() - points.getFirst().value();
            double baseline = Math.max(Math.abs(points.getFirst().value()), 1.0);
            if (Math.abs(delta) / baseline < 0.05) {
                return Direction.STABLE;
            }
            return delta > 0 ? Direction.INCREASING : Direction.DECREASING;
        }

        double mean = points.stream().mapToDouble(AnalyticsDtos.TrendPoint::value).average().orElse(0);
        double min = points.stream().mapToDouble(AnalyticsDtos.TrendPoint::value).min().orElse(0);
        double max = points.stream().mapToDouble(AnalyticsDtos.TrendPoint::value).max().orElse(0);
        double span = max - min;
        double relativeSpan = mean != 0 ? span / Math.abs(mean) : span;

        int directionChanges = 0;
        for (int i = 2; i < points.size(); i++) {
            double prevDelta = points.get(i - 1).value() - points.get(i - 2).value();
            double currDelta = points.get(i).value() - points.get(i - 1).value();
            if (prevDelta == 0 && currDelta == 0) {
                continue;
            }
            if ((prevDelta >= 0 && currDelta < 0) || (prevDelta <= 0 && currDelta > 0)) {
                directionChanges++;
            }
        }

        double totalChange = points.getLast().value() - points.getFirst().value();
        double baseline = Math.max(Math.abs(mean), 1.0);

        if (relativeSpan >= 0.12 && directionChanges >= 2) {
            return Direction.FLUCTUATING;
        }
        if (Math.abs(totalChange) / baseline < 0.05) {
            return Direction.STABLE;
        }
        return totalChange > 0 ? Direction.INCREASING : Direction.DECREASING;
    }

    private static String summarize(
            Direction direction,
            double latest,
            Double delta,
            Double percentChange
    ) {
        String change = "";
        if (delta != null && percentChange != null) {
            String sign = delta > 0 ? "up" : delta < 0 ? "down" : "unchanged";
            change = String.format(" Latest reading %.2f (%s %.1f%% vs prior).", latest, sign, Math.abs(percentChange));
        }

        return switch (direction) {
            case STABLE -> "Holding steady across recent reports." + change;
            case INCREASING -> "Trending upward over recent reports." + change;
            case DECREASING -> "Trending downward over recent reports." + change;
            case FLUCTUATING -> "Moving up and down — no clear direction yet." + change;
            case INSUFFICIENT_DATA -> "Not enough data for a trend.";
        };
    }
}
