package com.phi.analytics;

import com.phi.auth.AuthenticatedAccount;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsQueryService queryService;
    private final PersonChangeService changeService;
    private final PersonInsightService insightService;
    private final FamilyHealthSnapshotService familySnapshotService;
    private final RetestReminderService reminderService;

    public AnalyticsController(
            AnalyticsQueryService queryService,
            PersonChangeService changeService,
            PersonInsightService insightService,
            FamilyHealthSnapshotService familySnapshotService,
            RetestReminderService reminderService
    ) {
        this.queryService = queryService;
        this.changeService = changeService;
        this.insightService = insightService;
        this.familySnapshotService = familySnapshotService;
        this.reminderService = reminderService;
    }

    @GetMapping("/persons/{personId}/trends")
    public AnalyticsDtos.PersonTrendResponse personTrend(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId,
            @RequestParam String canonical
    ) {
        return queryService.personTrend(account, personId, canonical);
    }

    @GetMapping("/persons/{personId}/biomarkers")
    public List<String> availableBiomarkers(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId
    ) {
        return queryService.availableCanonicals(account, personId);
    }

    @GetMapping("/family/{familyId}/compare")
    public AnalyticsDtos.FamilyCompareResponse familyCompare(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable String familyId,
            @RequestParam String canonical
    ) {
        return queryService.familyCompare(account, familyId, canonical);
    }

    @GetMapping("/persons/{personId}/changes")
    public AnalyticsDtos.PersonChangesResponse personChanges(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId
    ) {
        return changeService.changesSincePreviousReport(account, personId);
    }

    @GetMapping("/persons/{personId}/insights")
    public AnalyticsDtos.PersonInsightsResponse personInsights(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId
    ) {
        return insightService.insights(account, personId);
    }

    @GetMapping("/persons/{personId}/reminders")
    public AnalyticsDtos.PersonRemindersResponse personReminders(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId
    ) {
        return reminderService.reminders(account, personId);
    }

    @GetMapping("/family/{familyId}/snapshot")
    public AnalyticsDtos.FamilyHealthSnapshot familySnapshot(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable String familyId
    ) {
        return familySnapshotService.snapshot(account, familyId);
    }

}
