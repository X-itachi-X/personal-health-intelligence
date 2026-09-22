package com.phi.analytics;

import java.util.List;

public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record TrendPoint(
            String date,
            double value,
            String unit,
            long reportId
    ) {
    }

    public record TrendInsight(
            String direction,
            String summary,
            int pointCount,
            Double latestValue,
            Double previousValue,
            Double delta,
            Double percentChange
    ) {
    }

    public record PersonTrendResponse(
            Long personId,
            String canonical,
            String unit,
            List<TrendPoint> points,
            TrendInsight insight
    ) {
    }

    public record FamilyCompareMember(
            Long personId,
            String personName,
            List<TrendPoint> points
    ) {
    }

    public record FamilyCompareResponse(
            String canonical,
            List<FamilyCompareMember> members
    ) {
    }

    public record RebuildResponse(
            int personsSynced,
            int biomarkersSynced,
            String message
    ) {
    }

    public record BiomarkerChange(
            String canonical,
            String testName,
            String unit,
            Double latestValue,
            Double previousValue,
            Double delta,
            Double percentChange,
            String direction,
            String referenceRange
    ) {
    }

    public record PersonChangesResponse(
            Long personId,
            Long latestReportId,
            String latestReportDate,
            Long previousReportId,
            String previousReportDate,
            int comparedBiomarkers,
            List<BiomarkerChange> changes
    ) {
    }

    public record InsightCard(
            String id,
            String type,
            String title,
            String message,
            String severity,
            String canonical,
            Long reportId
    ) {
    }

    public record PersonInsightsResponse(
            Long personId,
            List<InsightCard> cards
    ) {
    }

    public record RetestReminder(
            String id,
            String label,
            String canonical,
            String lastTestDate,
            String dueDate,
            String urgency,
            String reason,
            String message,
            Long reportId
    ) {
    }

    public record PersonRemindersResponse(
            Long personId,
            String summary,
            List<RetestReminder> reminders
    ) {
    }

    public record FamilyHealthSnapshot(
            String familyId,
            int memberCount,
            int reportsWithData,
            int outOfRangeMembers,
            List<MemberSnapshot> members
    ) {
    }

    public record MemberSnapshot(
            Long personId,
            String personName,
            int outOfRangeCount,
            String latestReportDate
    ) {
    }
}
