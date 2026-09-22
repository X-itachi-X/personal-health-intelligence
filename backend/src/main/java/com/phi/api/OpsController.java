package com.phi.api;

import com.phi.access.PlatformAccessService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.feedback.FeedbackDtos;
import com.phi.feedback.FeedbackService;
import com.phi.ops.OpsDtos;
import com.phi.ops.OpsFreeToolService;
import com.phi.ops.OpsQueryService;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

/**
 * Platform operator console — app-wide monitoring across all testers/families.
 * Not visible to regular users. Requires {@code PHI_PLATFORM_ADMIN_EMAILS}.
 */
@RestController
@RequestMapping("/api/v1/ops")
public class OpsController {

    private final OpsQueryService opsQueryService;
    private final OpsFreeToolService opsFreeToolService;
    private final FeedbackService feedbackService;
    private final PlatformAccessService platformAccess;

    public OpsController(
            OpsQueryService opsQueryService,
            OpsFreeToolService opsFreeToolService,
            FeedbackService feedbackService,
            PlatformAccessService platformAccess
    ) {
        this.opsQueryService = opsQueryService;
        this.opsFreeToolService = opsFreeToolService;
        this.feedbackService = feedbackService;
        this.platformAccess = platformAccess;
    }

    @GetMapping("/summary")
    public OpsDtos.OpsSummary summary(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestParam(defaultValue = "7") int days
    ) {
        platformAccess.requirePlatformAdmin(account);
        return opsQueryService.summary(Math.min(Math.max(days, 1), 90));
    }

    @GetMapping("/timeseries")
    public OpsDtos.OpsTimeseries timeseries(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestParam(defaultValue = "7") int days
    ) {
        platformAccess.requirePlatformAdmin(account);
        return opsQueryService.timeseries(Math.min(Math.max(days, 1), 90));
    }

    @GetMapping("/events")
    public List<OpsDtos.ExtractionEventView> recentEvents(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestParam(required = false) String familyId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        platformAccess.requirePlatformAdmin(account);
        return opsQueryService.recentEventsGlobal(Math.min(Math.max(limit, 1), 200), familyId);
    }

    @GetMapping("/errors")
    public List<OpsDtos.ExtractionEventView> recentErrors(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "30") int limit
    ) {
        platformAccess.requirePlatformAdmin(account);
        return opsQueryService.recentErrors(days, Math.min(Math.max(limit, 1), 100));
    }

    @GetMapping("/reports")
    public List<OpsDtos.ReportOpsRow> recentReports(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestParam(defaultValue = "50") int limit
    ) {
        platformAccess.requirePlatformAdmin(account);
        return opsQueryService.recentReports(Math.min(Math.max(limit, 1), 200));
    }

    @GetMapping("/reports/{reportId}/pipeline")
    public OpsDtos.ReportPipelineView reportPipeline(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId
    ) {
        platformAccess.requirePlatformAdmin(account);
        return opsQueryService.reportPipeline(reportId);
    }

    @GetMapping("/reports/{reportId}/file")
    public ResponseEntity<ByteArrayResource> downloadOriginalFile(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId
    ) {
        platformAccess.requirePlatformAdmin(account);
        OpsFreeToolService.OpsFileResource file = opsFreeToolService.loadOriginalFile(reportId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.sizeBytes())
                .body(new ByteArrayResource(file.bytes()));
    }

    @GetMapping("/reports/{reportId}/free-tools")
    public OpsDtos.FreeToolComparisonView compareFreeTools(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId,
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        platformAccess.requirePlatformAdmin(account);
        return opsFreeToolService.compareFreeTools(reportId, refresh);
    }

    @GetMapping("/audit")
    public List<OpsDtos.AuditEventView> auditLog(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestParam(required = false) String familyId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        platformAccess.requirePlatformAdmin(account);
        return opsQueryService.recentAuditGlobal(Math.min(Math.max(limit, 1), 200), familyId);
    }

    @GetMapping("/feedback")
    public List<FeedbackDtos.FeedbackView> testerFeedback(
            @AuthenticationPrincipal AuthenticatedAccount account
    ) {
        platformAccess.requirePlatformAdmin(account);
        return feedbackService.listForOps();
    }

    @PatchMapping("/feedback/{feedbackId}")
    public FeedbackDtos.FeedbackView updateFeedbackStatus(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable String feedbackId,
            @Valid @RequestBody FeedbackDtos.UpdateStatusRequest request
    ) {
        platformAccess.requirePlatformAdmin(account);
        return feedbackService.updateStatus(feedbackId, request.status());
    }
}
