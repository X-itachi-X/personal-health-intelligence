package com.phi.telemetry;

import com.phi.domain.ExtractionEvent;
import com.phi.domain.ExtractionEventRepository;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.extraction.ClaudeApiUsage;
import com.phi.extraction.TextExtractionMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtractionTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionTelemetryService.class);

    private final ExtractionEventRepository eventRepository;
    private final LabReportRepository labReportRepository;

    public ExtractionTelemetryService(
            ExtractionEventRepository eventRepository,
            LabReportRepository labReportRepository
    ) {
        this.eventRepository = eventRepository;
        this.labReportRepository = labReportRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pipelineStarted(Long reportId) {
        logEvent(reportId, "pipeline.started", "info", null, null, null, null, null, null, null,
                "Extraction pipeline started", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void textExtracted(
            Long reportId,
            TextExtractionMethod method,
            long durationMs,
            String text,
            boolean lowQuality
    ) {
        String eventType = eventTypeForMethod(method);
        String status = lowQuality ? "warning" : "success";
        int charCount = text != null ? text.length() : 0;
        String message = lowQuality
                ? "Text extracted but OCR quality is low — verify results"
                : "Text extracted via " + method.name();
        String metadata = lowQuality
                ? TelemetryMetadata.withTextPreviewAndFlag(text, "lowQuality", true)
                : TelemetryMetadata.withTextPreview(text);
        logEvent(reportId, eventType, status, durationMs, charCount, null, null, null, null, null, message, metadata);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void textFromStored(Long reportId, String text) {
        int charCount = text != null ? text.length() : 0;
        logEvent(reportId, "text.stored_retry", "info", null, charCount, null, null, null, null, null,
                "Using previously stored extracted text", TelemetryMetadata.withTextPreview(text));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void visionTextExtracted(Long reportId, long durationMs, String text, ClaudeApiUsage usage) {
        int charCount = text != null ? text.length() : 0;
        logEvent(reportId, "text.ai_vision", "warning", durationMs, charCount, null, null,
                usage.inputTokens(), usage.outputTokens(), usage.model(),
                "Claude vision used as last-resort text extraction", TelemetryMetadata.withTextPreview(text));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rulesAttempt(Long reportId, String labFormat, double coverage, int biomarkerCount, boolean sufficient) {
        String status = sufficient ? "success" : "info";
        String message = sufficient
                ? "Rules sufficient — skipped Claude (" + labFormat + ")"
                : "Rules insufficient — Claude may be needed (" + labFormat + ")";
        logEvent(reportId, "parse.rules", status, null, null, biomarkerCount, coverage,
                null, null, null, message, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void claudeParse(Long reportId, long durationMs, int biomarkerCount, ClaudeApiUsage usage) {
        logEvent(reportId, "parse.claude", "warning", durationMs, null, biomarkerCount, null,
                usage.inputTokens(), usage.outputTokens(), usage.model(),
                "Claude biomarker parse (last resort)", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reportDateExtracted(Long reportId, String reportDate) {
        logEvent(reportId, "report.date.extracted", "success", null, null, null, null,
                null, null, null, "Report date extracted from document: " + reportDate, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reportDateRequired(Long reportId) {
        logEvent(reportId, "report.date.required", "warning", null, null, null, null,
                null, null, null, "Report date could not be extracted — user input required", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reportDateProvided(Long reportId, String reportDate, boolean userSupplied) {
        String message = userSupplied
                ? "Report date provided by user: " + reportDate
                : "Report date set: " + reportDate;
        logEvent(reportId, "report.date.provided", "success", null, null, null, null,
                null, null, null, message, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void parseSkipped(Long reportId, String reason) {
        logEvent(reportId, "parse.skipped", "info", null, null, null, null, null, null, null, reason, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void filePurged(Long reportId) {
        logEvent(reportId, "file.purged", "info", null, null, null, null, null, null, null,
                "Ingest file purged after text extraction", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pipelineCompleted(Long reportId, int biomarkerCount) {
        logEvent(reportId, "pipeline.completed", "success", null, null, biomarkerCount, null,
                null, null, null, "Extraction completed", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pipelineFailed(Long reportId, String error) {
        String message = error != null && error.length() > 500 ? error.substring(0, 500) : error;
        logEvent(reportId, "pipeline.failed", "error", null, null, null, null,
                null, null, null, message, null);
        log.warn("Report {} pipeline failed: {}", reportId, message);
    }

    private void logEvent(
            Long reportId,
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
            String metadata
    ) {
        try {
            String familyId = labReportRepository.findById(reportId)
                    .map(LabReport::getFamilyId)
                    .orElse(null);
            eventRepository.save(new ExtractionEvent(
                    reportId,
                    familyId,
                    eventType,
                    status,
                    durationMs,
                    charCount,
                    biomarkerCount,
                    coverage,
                    inputTokens,
                    outputTokens,
                    model,
                    message,
                    metadata
            ));
            log.info("telemetry report={} type={} status={} msg={}", reportId, eventType, status, message);
        } catch (Exception e) {
            log.error(
                    "Failed to persist extraction telemetry for report {} (type={}): {}",
                    reportId,
                    eventType,
                    e.getMessage(),
                    e
            );
        }
    }

    private static String eventTypeForMethod(TextExtractionMethod method) {
        return switch (method) {
            case PDF_TEXT_LAYER -> "text.pdf_layer";
            case OCR_PDF -> "text.ocr_pdf";
            case OCR_IMAGE -> "text.ocr_image";
            case PLAIN_TEXT -> "text.plain";
            case AI_VISION -> "text.ai_vision";
        };
    }
}
