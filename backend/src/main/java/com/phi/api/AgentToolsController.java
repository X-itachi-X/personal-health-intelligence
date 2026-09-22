package com.phi.api;

import com.phi.analytics.AnalyticsDtos;
import com.phi.analytics.AnalyticsQueryService;
import com.phi.analytics.MedicationCorrelationService;
import com.phi.analytics.PersonChangeService;
import com.phi.imaging.ImagingDtos;
import com.phi.imaging.ImagingQueryService;
import com.phi.imaging.ImagingCorrelationService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.medication.MedicationDtos;
import com.phi.medication.MedicationService;
import com.phi.reasoning.ReportFindingsService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SQL-grounded tool endpoints for a future chat agent. No PDF dumps.
 */
@RestController
@RequestMapping("/api/v1/agent/tools")
public class AgentToolsController {

    private final AnalyticsQueryService analyticsQueryService;
    private final PersonChangeService changeService;
    private final ReportFindingsService findingsService;
    private final MedicationService medicationService;
    private final MedicationCorrelationService medicationCorrelationService;
    private final ImagingQueryService imagingQueryService;
    private final ImagingCorrelationService imagingCorrelationService;

    public AgentToolsController(
            AnalyticsQueryService analyticsQueryService,
            PersonChangeService changeService,
            ReportFindingsService findingsService,
            MedicationService medicationService,
            MedicationCorrelationService medicationCorrelationService,
            ImagingQueryService imagingQueryService,
            ImagingCorrelationService imagingCorrelationService
    ) {
        this.analyticsQueryService = analyticsQueryService;
        this.changeService = changeService;
        this.findingsService = findingsService;
        this.medicationService = medicationService;
        this.medicationCorrelationService = medicationCorrelationService;
        this.imagingQueryService = imagingQueryService;
        this.imagingCorrelationService = imagingCorrelationService;
    }

    @GetMapping("/persons/{personId}/timeline")
    public AnalyticsDtos.PersonTrendResponse timeline(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId,
            @RequestParam String canonical
    ) {
        return analyticsQueryService.personTrend(account, personId, canonical);
    }

    @GetMapping("/persons/{personId}/changes")
    public AnalyticsDtos.PersonChangesResponse changes(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId
    ) {
        return changeService.changesSincePreviousReport(account, personId);
    }

    @GetMapping("/reports/{reportId}/abnormal")
    public ReportFindingsService.FindingsResponse abnormal(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId
    ) {
        return findingsService.findingsForReport(reportId);
    }

    @GetMapping("/persons/{personId}/medications")
    public List<MedicationDtos.MedicationView> medications(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId
    ) {
        return medicationService.list(account, personId);
    }

    @GetMapping("/persons/{personId}/medication-context")
    public List<AnalyticsDtos.InsightCard> medicationContext(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId
    ) {
        return medicationCorrelationService.correlationCards(account, personId);
    }

    @GetMapping("/persons/{personId}/available-biomarkers")
    public List<String> availableBiomarkers(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId
    ) {
        return analyticsQueryService.availableCanonicals(account, personId);
    }

    @GetMapping("/persons/{personId}/imaging-studies")
    public List<ImagingDtos.StudyView> imagingStudies(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId
    ) {
        return imagingQueryService.listForPerson(account, personId);
    }

    @GetMapping("/persons/{personId}/imaging-context")
    public List<com.phi.analytics.AnalyticsDtos.InsightCard> imagingContext(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId
    ) {
        return imagingCorrelationService.correlationCards(account, personId);
    }

    @GetMapping("/catalog")
    public Map<String, Object> catalog() {
        Map<String, Object> tools = new LinkedHashMap<>();
        tools.put("timeline", "GET /api/v1/agent/tools/persons/{personId}/timeline?canonical=");
        tools.put("changes", "GET /api/v1/agent/tools/persons/{personId}/changes");
        tools.put("abnormal", "GET /api/v1/agent/tools/reports/{reportId}/abnormal");
        tools.put("medications", "GET /api/v1/agent/tools/persons/{personId}/medications");
        tools.put("medicationContext", "GET /api/v1/agent/tools/persons/{personId}/medication-context");
        tools.put("imagingStudies", "GET /api/v1/agent/tools/persons/{personId}/imaging-studies");
        tools.put("imagingContext", "GET /api/v1/agent/tools/persons/{personId}/imaging-context");
        tools.put("availableBiomarkers", "GET /api/v1/agent/tools/persons/{personId}/available-biomarkers");
        return Map.of("tools", tools);
    }
}
