package com.phi.analytics;

import com.phi.auth.AuthenticatedAccount;
import com.phi.imaging.ImagingCorrelationService;
import com.phi.reasoning.ReferenceRangeAssessor;
import com.phi.reasoning.ReportFindingsService;
import com.phi.reasoning.TrendInsightAnalyzer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonInsightService {

    private static final int MAX_CHANGE_CARDS = 3;
    private static final List<String> KEY_BIOMARKERS = List.of(
            "fasting_glucose",
            "hba1c",
            "ldl_cholesterol",
            "hdl_cholesterol",
            "triglycerides",
            "vitamin_d",
            "hemoglobin",
            "tsh"
    );

    private final PersonChangeService changeService;
    private final AnalyticsQueryService analyticsQueryService;
    private final ReportFindingsService findingsService;
    private final MedicationCorrelationService medicationCorrelationService;
    private final ImagingCorrelationService imagingCorrelationService;

    public PersonInsightService(
            PersonChangeService changeService,
            AnalyticsQueryService analyticsQueryService,
            ReportFindingsService findingsService,
            MedicationCorrelationService medicationCorrelationService,
            ImagingCorrelationService imagingCorrelationService
    ) {
        this.changeService = changeService;
        this.analyticsQueryService = analyticsQueryService;
        this.findingsService = findingsService;
        this.medicationCorrelationService = medicationCorrelationService;
        this.imagingCorrelationService = imagingCorrelationService;
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.PersonInsightsResponse insights(AuthenticatedAccount account, Long personId) {
        List<AnalyticsDtos.InsightCard> cards = new ArrayList<>();

        AnalyticsDtos.PersonChangesResponse changes = changeService.changesSincePreviousReport(account, personId);
        if (changes.latestReportId() != null) {
            var findings = findingsService.findingsForReport(changes.latestReportId());
            for (var finding : findings.findings()) {
                if (!"CLINICAL_RULE".equals(finding.kind())) {
                    continue;
                }
                cards.add(new AnalyticsDtos.InsightCard(
                        "rule-" + finding.ruleId(),
                        "CLINICAL_RULE",
                        formatName(finding.canonical() != null ? finding.canonical() : "Health signal"),
                        finding.message(),
                        severityForRule(finding.severity()),
                        finding.canonical(),
                        changes.latestReportId()
                ));
            }

            long referenceFindings = findings.findings().stream()
                    .filter(finding -> "REFERENCE_RANGE".equals(finding.kind()))
                    .count();
            if (referenceFindings > 0) {
                cards.add(new AnalyticsDtos.InsightCard(
                        "out-of-range",
                        "OUT_OF_RANGE",
                        referenceFindings + " results outside reference range",
                        referenceFindings == 1
                                ? "One biomarker in your latest report is outside the printed reference range."
                                : referenceFindings + " biomarkers in your latest report are outside the printed reference range.",
                        "warning",
                        null,
                        changes.latestReportId()
                ));
            }
        }

        for (AnalyticsDtos.InsightCard medCard : medicationCorrelationService.correlationCards(account, personId)) {
            cards.add(medCard);
        }

        for (AnalyticsDtos.InsightCard imagingCard : imagingCorrelationService.correlationCards(account, personId)) {
            cards.add(imagingCard);
        }

        if (changes.previousReportDate() != null) {
            changes.changes().stream()
                    .filter(change -> change.direction() != null
                            && (change.direction().equals("UP") || change.direction().equals("DOWN")))
                    .filter(change -> change.percentChange() != null && Math.abs(change.percentChange()) >= 5)
                    .limit(MAX_CHANGE_CARDS)
                    .forEach(change -> cards.add(changeCard(changes, change)));
        } else if (changes.latestReportDate() != null) {
            cards.add(new AnalyticsDtos.InsightCard(
                    "first-report",
                    "INFO",
                    "First report on file",
                    "Upload another lab report to see what changed between tests.",
                    "info",
                    null,
                    changes.latestReportId()
            ));
        }

        Set<String> trendCanonicals = new LinkedHashSet<>(KEY_BIOMARKERS);
        changes.changes().stream()
                .map(AnalyticsDtos.BiomarkerChange::canonical)
                .limit(5)
                .forEach(trendCanonicals::add);

        for (String canonical : trendCanonicals) {
            AnalyticsDtos.PersonTrendResponse trend = analyticsQueryService.personTrend(account, personId, canonical);
            if (trend.insight() == null
                    || trend.insight().direction().equals("INSUFFICIENT_DATA")
                    || trend.points().size() < 3) {
                continue;
            }
            String direction = trend.insight().direction();
            if (direction.equals("STABLE")) {
                continue;
            }
            cards.add(new AnalyticsDtos.InsightCard(
                    "trend-" + canonical,
                    "TREND",
                    formatName(canonical) + " — " + trendLabel(direction),
                    trend.insight().summary(),
                    direction.equals("FLUCTUATING") ? "warning" : "info",
                    canonical,
                    trend.points().getLast().reportId()
            ));
            if (cards.size() >= 8) {
                break;
            }
        }

        return new AnalyticsDtos.PersonInsightsResponse(personId, cards);
    }

    private static AnalyticsDtos.InsightCard changeCard(
            AnalyticsDtos.PersonChangesResponse changes,
            AnalyticsDtos.BiomarkerChange change
    ) {
        String name = change.testName() != null && !change.testName().isBlank()
                ? change.testName()
                : formatName(change.canonical());
        String unit = change.unit() != null ? " " + change.unit() : "";
        String arrow = change.direction().equals("UP") ? "up" : "down";
        String message = String.format(
                "%s moved from %.2f to %.2f%s (%s %.1f%%) between %s and %s.",
                name,
                change.previousValue(),
                change.latestValue(),
                unit,
                arrow,
                Math.abs(change.percentChange()),
                changes.previousReportDate(),
                changes.latestReportDate()
        );

        ReferenceRangeAssessor.Status status = ReferenceRangeAssessor.assess(
                java.math.BigDecimal.valueOf(change.latestValue()),
                change.referenceRange()
        );
        String severity = status == ReferenceRangeAssessor.Status.HIGH
                || status == ReferenceRangeAssessor.Status.LOW
                ? "warning"
                : "info";

        return new AnalyticsDtos.InsightCard(
                "change-" + change.canonical(),
                "CHANGE",
                name + " " + arrow,
                message,
                severity,
                change.canonical(),
                changes.latestReportId()
        );
    }

    private static String trendLabel(String direction) {
        return switch (direction) {
            case "INCREASING" -> "trending up";
            case "DECREASING" -> "trending down";
            case "FLUCTUATING" -> "fluctuating";
            case "STABLE" -> "holding steady";
            default -> direction.toLowerCase();
        };
    }

    private static String formatName(String canonical) {
        return canonical.replace('_', ' ');
    }

    private static String severityForRule(String severity) {
        if (severity == null) {
            return "info";
        }
        return switch (severity.toLowerCase()) {
            case "high" -> "warning";
            case "medium", "low" -> "info";
            default -> "info";
        };
    }
}
