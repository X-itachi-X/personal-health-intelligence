package com.phi.ops;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class OpsDtos {

    private OpsDtos() {
    }

    public record ExtractionEventView(
            String id,
            Long reportId,
            String familyId,
            String eventType,
            String status,
            Long durationMs,
            Integer charCount,
            Integer biomarkerCount,
            Double coverage,
            Integer inputTokens,
            Integer outputTokens,
            String model,
            String message,
            String textPreview,
            Instant createdAt
    ) {
    }

    public record ReportPipelineView(
            Long reportId,
            String filename,
            String extractionStatus,
            String extractionError,
            Instant uploadedAt,
            String textExtractionTool,
            String biomarkerParseTool,
            String extractedText,
            int extractedTextLength,
            boolean originalFileAvailable,
            Long originalFileSizeBytes,
            String originalContentType,
            List<ExtractionEventView> events,
            int totalInputTokens,
            int totalOutputTokens
    ) {
    }

    public record BiomarkerPreview(
            String canonical,
            String testName,
            String value,
            String unit
    ) {
    }

    public record FreeToolResult(
            String toolId,
            String label,
            String category,
            String sourceToolId,
            String status,
            Long durationMs,
            Integer charCount,
            Integer biomarkerCount,
            Double coverage,
            String labFormat,
            String text,
            String error,
            List<BiomarkerPreview> biomarkers
    ) {
    }

    public record FreeToolComparisonView(
            Long reportId,
            String filename,
            boolean originalFileAvailable,
            Long originalFileSizeBytes,
            String originalContentType,
            String fileUnavailableReason,
            List<FreeToolResult> tools
    ) {
    }

    public record OpsSummary(
            int days,
            long totalEvents,
            int totalInputTokens,
            int totalOutputTokens,
            int aiParseCalls,
            int aiVisionCalls,
            int ruleSuccesses,
            int failures,
            Map<String, Long> eventsByType
    ) {
    }

    public record OpsDayBucket(
            String date,
            int totalTokens,
            int failures,
            int uploads
    ) {
    }

    public record OpsTimeseries(
            int days,
            List<OpsDayBucket> buckets
    ) {
    }

    public record ReportOpsRow(
            Long reportId,
            String filename,
            String extractionStatus,
            String extractionError,
            Long personId,
            String personName,
            String familyId,
            String uploadedByAccountId,
            Instant uploadedAt
    ) {
    }

    public record AuditEventView(
            String id,
            String action,
            String actorAccountId,
            String targetType,
            String targetId,
            String metadata,
            Instant createdAt
    ) {
    }
}
