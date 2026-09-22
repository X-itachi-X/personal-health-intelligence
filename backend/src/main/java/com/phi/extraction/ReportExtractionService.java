package com.phi.extraction;

import com.phi.analytics.AnalyticsSyncService;
import com.phi.config.PhiProperties;
import com.phi.domain.BiomarkerValue;
import com.phi.domain.BiomarkerValueRepository;
import com.phi.domain.ExtractionEvent;
import com.phi.domain.ExtractionEventRepository;
import com.phi.domain.ExtractionStatus;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.ReportDateSource;
import com.phi.extraction.rules.RuleExtractionService;
import com.phi.imaging.ImagingExtractionService;
import com.phi.prescription.PrescriptionExtractionService;
import com.phi.storage.ReportFileStorageService;
import com.phi.telemetry.ExtractionTelemetryService;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ReportExtractionService.class);

    private final PhiProperties properties;
    private final LabReportRepository labReportRepository;
    private final BiomarkerValueRepository biomarkerValueRepository;
    private final DocumentTextExtractor documentTextExtractor;
    private final TextQualityAssessor textQualityAssessor;
    private final ClaudeExtractionClient claudeClient;
    private final ClaudeVisionTextClient claudeVisionClient;
    private final BiomarkerNormalizer normalizer;
    private final AnalyticsSyncService analyticsSyncService;
    private final ReportFileStorageService fileStorageService;
    private final RuleExtractionService ruleExtractionService;
    private final ReportDateExtractor reportDateExtractor;
    private final ExtractionEventRepository extractionEventRepository;
    private final ExtractionTelemetryService telemetry;
    private final PrescriptionExtractionService prescriptionExtractionService;
    private final ImagingExtractionService imagingExtractionService;
    private final ReportExtractionService self;

    public ReportExtractionService(
            PhiProperties properties,
            LabReportRepository labReportRepository,
            BiomarkerValueRepository biomarkerValueRepository,
            DocumentTextExtractor documentTextExtractor,
            TextQualityAssessor textQualityAssessor,
            ClaudeExtractionClient claudeClient,
            ClaudeVisionTextClient claudeVisionClient,
            BiomarkerNormalizer normalizer,
            AnalyticsSyncService analyticsSyncService,
            ReportFileStorageService fileStorageService,
            RuleExtractionService ruleExtractionService,
            ReportDateExtractor reportDateExtractor,
            ExtractionEventRepository extractionEventRepository,
            ExtractionTelemetryService telemetry,
            PrescriptionExtractionService prescriptionExtractionService,
            ImagingExtractionService imagingExtractionService,
            @Lazy ReportExtractionService self
    ) {
        this.properties = properties;
        this.labReportRepository = labReportRepository;
        this.biomarkerValueRepository = biomarkerValueRepository;
        this.documentTextExtractor = documentTextExtractor;
        this.textQualityAssessor = textQualityAssessor;
        this.claudeClient = claudeClient;
        this.claudeVisionClient = claudeVisionClient;
        this.normalizer = normalizer;
        this.analyticsSyncService = analyticsSyncService;
        this.fileStorageService = fileStorageService;
        this.ruleExtractionService = ruleExtractionService;
        this.reportDateExtractor = reportDateExtractor;
        this.extractionEventRepository = extractionEventRepository;
        this.telemetry = telemetry;
        this.prescriptionExtractionService = prescriptionExtractionService;
        this.imagingExtractionService = imagingExtractionService;
        this.self = self;
    }

    @Transactional
    public void submitReportDate(Long reportId, LocalDate reportDate) {
        applyUserReportDate(reportId, reportDate, true);
    }

    @Transactional
    public void correctReportDate(Long reportId, LocalDate reportDate) {
        validateReportDate(reportDate);
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        if (report.getExtractionStatus() != ExtractionStatus.COMPLETED
                && report.getExtractionStatus() != ExtractionStatus.TEXT_ONLY
                && report.getExtractionStatus() != ExtractionStatus.AWAITING_REPORT_DATE
                && report.getExtractionStatus() != ExtractionStatus.TEXT_EXTRACTED) {
            throw new IllegalStateException("Report date cannot be corrected in the current state");
        }

        report.setReportDate(reportDate);
        report.setReportDateSource(ReportDateSource.USER);
        if (report.getExtractionStatus() == ExtractionStatus.AWAITING_REPORT_DATE) {
            String text = report.getExtractedText();
            if (text != null && !text.isBlank()) {
                report.markTextExtracted(text);
            }
        }
        labReportRepository.save(report);
        telemetry.reportDateProvided(reportId, reportDate.toString(), true);

        if (report.getExtractionStatus() == ExtractionStatus.COMPLETED
                || report.getExtractionStatus() == ExtractionStatus.TEXT_ONLY) {
            analyticsSyncService.syncReport(reportId);
        }
    }

    private void applyUserReportDate(Long reportId, LocalDate reportDate, boolean resumeTextExtracted) {
        validateReportDate(reportDate);

        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        if (report.getExtractionStatus() != ExtractionStatus.AWAITING_REPORT_DATE
                && report.getExtractionStatus() != ExtractionStatus.TEXT_EXTRACTED
                && report.getExtractionStatus() != ExtractionStatus.TEXT_ONLY) {
            throw new IllegalStateException("Report date can only be set before biomarker parsing completes");
        }

        report.setReportDate(reportDate);
        report.setReportDateSource(ReportDateSource.USER);
        if (resumeTextExtracted) {
            String text = report.getExtractedText();
            if (text != null && !text.isBlank()) {
                report.markTextExtracted(text);
            }
        }
        labReportRepository.save(report);
        telemetry.reportDateProvided(reportId, reportDate.toString(), true);
    }

    public void submitReportDateAndContinue(Long reportId, LocalDate reportDate) {
        self.submitReportDate(reportId, reportDate);
        self.continuePipelineAsync(reportId);
    }

    public void correctReportDateAndContinue(Long reportId, LocalDate reportDate) {
        self.correctReportDate(reportId, reportDate);
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        if (report.getExtractionStatus() == ExtractionStatus.TEXT_EXTRACTED
                || report.getExtractionStatus() == ExtractionStatus.AWAITING_REPORT_DATE) {
            self.continuePipelineAsync(reportId);
        }
    }

    private static void validateReportDate(LocalDate reportDate) {
        if (reportDate == null) {
            throw new IllegalArgumentException("Report date is required");
        }
        if (reportDate.isAfter(LocalDate.now().plusDays(1))) {
            throw new IllegalArgumentException("Report date cannot be in the future");
        }
    }

    @Transactional
    public void retryExtraction(Long reportId) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        if (report.getExtractionStatus() == ExtractionStatus.PROCESSING) {
            throw new IllegalStateException("Extraction already in progress");
        }

        if (fileStorageService.canRetryFromStoredText(report)) {
            report.markProcessing();
            labReportRepository.save(report);
            runBiomarkerParsingAsync(reportId);
            return;
        }

        if (!fileStorageService.hasReadableFile(report)) {
            throw new IllegalStateException(
                    "Report has no stored file or extracted text to retry. Replace the file or delete and re-upload."
            );
        }

        biomarkerValueRepository.deleteByLabReportId(reportId);
        report.markProcessing();
        labReportRepository.save(report);
        extractAsync(reportId);
    }

    public void parseWithClaudeAsync(Long reportId) {
        requireReadyForBiomarkerParsing(reportId);
        self.runBiomarkerParsing(reportId);
    }

    public void runBiomarkerParsingAsync(Long reportId) {
        requireReadyForBiomarkerParsing(reportId);
        self.runBiomarkerParsing(reportId);
    }

    @Async
    public void runBiomarkerParsing(Long reportId) {
        try {
            self.markParsing(reportId);
            String text = labReportRepository.findById(reportId)
                    .orElseThrow()
                    .getExtractedText();

            if (tryRulesExtraction(reportId, text)) {
                return;
            }

            if (!claudeClient.isConfigured()) {
                self.restoreTextExtracted(reportId, text);
                telemetry.parseSkipped(reportId, "Rules insufficient and Claude not configured");
                log.info("Report {} rules insufficient and Claude not configured — kept as TEXT_EXTRACTED", reportId);
                return;
            }

            log.info("Report {} rules insufficient — Claude biomarker parse as last resort", reportId);
            long start = System.currentTimeMillis();
            ClaudeExtractionResult result = claudeClient.extractBiomarkers(text);
            telemetry.claudeParse(
                    reportId,
                    System.currentTimeMillis() - start,
                    result.biomarkers().size(),
                    result.usage()
            );
            self.completeExtraction(reportId, text, result.biomarkers());
        } catch (Exception e) {
            log.error("Biomarker parsing failed for report {}", reportId, e);
            self.markFailed(reportId, e.getMessage());
        }
    }

    private boolean tryRulesExtraction(Long reportId, String text) {
        return ruleExtractionService.tryExtract(text)
                .map(result -> {
                    boolean sufficient = result.meetsCoverage(properties.claude().effectiveMinCoverage());
                    telemetry.rulesAttempt(
                            reportId,
                            result.labFormat(),
                            result.coverage(),
                            result.biomarkers().size(),
                            sufficient
                    );
                    if (!sufficient) {
                        return false;
                    }
                    self.completeExtraction(reportId, text, result.biomarkers());
                    return true;
                })
                .orElse(false);
    }

    private void requireReadyForBiomarkerParsing(Long reportId) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        if (!fileStorageService.canRetryFromStoredText(report)) {
            throw new IllegalStateException("Report has no extracted text to parse");
        }
        if (report.getExtractionStatus() == ExtractionStatus.PROCESSING) {
            throw new IllegalStateException("Extraction already in progress");
        }
        requireReportDate(report);
    }

    private static void requireReportDate(LabReport report) {
        if (!report.hasReportDate()) {
            throw new IllegalStateException("Report date is required before biomarker parsing");
        }
    }

    @Async
    public void extractAsync(Long reportId) {
        telemetry.pipelineStarted(reportId);
        try {
            if (!self.extractAndPersistLocalText(reportId)) {
                return;
            }
            self.continuePipelineAfterText(reportId);
        } catch (Exception e) {
            log.error("Local extraction failed for report {}", reportId, e);
            self.markFailed(reportId, e.getMessage());
        }
    }

    @Async
    public void continuePipelineAsync(Long reportId) {
        try {
            self.continuePipelineAfterText(reportId);
        } catch (Exception e) {
            log.error("Pipeline continuation failed for report {}", reportId, e);
            self.markFailed(reportId, e.getMessage());
        }
    }

    @Transactional
    public void continuePipelineAfterText(Long reportId) {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        if (report.isImagingReport()) {
            if (!self.ensureReportDateResolved(reportId)) {
                return;
            }
            imagingExtractionService.processAfterText(reportId);
            return;
        }

        if (report.isPrescription()) {
            if (!self.ensureReportDateResolved(reportId)) {
                return;
            }
            prescriptionExtractionService.processAfterText(reportId);
            return;
        }

        if (!self.ensureReportDateResolved(reportId)) {
            return;
        }

        if (self.tryCompleteWithRules(reportId)) {
            return;
        }

        if (!claudeClient.isConfigured()) {
            self.finalizeTextOnly(reportId);
            telemetry.parseSkipped(reportId, "Claude not configured — saved as text-only");
            return;
        }

        if (properties.claude().autoParseEnabled()) {
            self.runBiomarkerParsing(reportId);
        } else {
            telemetry.parseSkipped(reportId, "Claude auto-parse disabled — manual POST /parse required");
            log.info("Report {} text saved; rules insufficient, Claude auto-parse disabled", reportId);
        }
    }

    @Transactional
    public boolean ensureReportDateResolved(Long reportId) {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        if (report.hasReportDate()) {
            return true;
        }

        String text = report.getExtractedText();
        if (usedOcrForTextExtraction(reportId)) {
            report.markAwaitingReportDate(text);
            labReportRepository.save(report);
            telemetry.reportDateRequired(reportId);
            log.info("Report {} awaiting user-provided report date (OCR/image upload)", reportId);
            return false;
        }

        Optional<LocalDate> extracted = reportDateExtractor.extract(text);
        if (extracted.isPresent()) {
            report.setReportDate(extracted.get());
            report.setReportDateSource(ReportDateSource.EXTRACTED);
            if (text != null && !text.isBlank()) {
                report.markTextExtracted(text);
            }
            labReportRepository.save(report);
            telemetry.reportDateExtracted(reportId, extracted.get().toString());
            log.info("Report {} report date extracted: {}", reportId, extracted.get());
            return true;
        }

        report.markAwaitingReportDate(text);
        labReportRepository.save(report);
        telemetry.reportDateRequired(reportId);
        log.info("Report {} awaiting user-provided report date", reportId);
        return false;
    }

    private boolean usedOcrForTextExtraction(Long reportId) {
        return extractionEventRepository.findByLabReportIdOrderByCreatedAtAsc(reportId).stream()
                .filter(event -> event.getEventType().startsWith("text."))
                .reduce((first, second) -> second)
                .map(ExtractionEvent::getEventType)
                .filter(type -> "text.ocr_image".equals(type) || "text.ocr_pdf".equals(type))
                .isPresent();
    }

    /**
     * Phase 1 — local PDF/image text extraction only. Persists {@code extracted_text} before any Claude call.
     */
    @Transactional
    public boolean extractAndPersistLocalText(Long reportId) throws Exception {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        if (fileStorageService.canRetryFromStoredText(report)
                && (report.getExtractionStatus() == ExtractionStatus.TEXT_EXTRACTED
                || report.getExtractionStatus() == ExtractionStatus.AWAITING_REPORT_DATE)) {
            return true;
        }

        report.markProcessing();
        labReportRepository.save(report);

        String text = extractTextFromSource(report);

        if (text == null || text.isBlank()) {
            report.markFailed("No text could be extracted from the document");
            labReportRepository.save(report);
            telemetry.pipelineFailed(reportId, "No text could be extracted from the document");
            return false;
        }

        report.markTextExtracted(text);
        labReportRepository.save(report);
        boolean hadFile = fileStorageService.hasReadableFile(report);
        fileStorageService.purgeAfterExtractionIfDue(report);
        if (hadFile && properties.storage().effectiveRetainFilesDays() == 0) {
            telemetry.filePurged(reportId);
        }
        log.info("Report {} local text extracted ({} chars)", reportId, text.length());
        return true;
    }

    @Transactional
    public void markParsing(Long reportId) {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        report.markProcessing();
        labReportRepository.save(report);
    }

    @Transactional
    public boolean tryCompleteWithRules(Long reportId) {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        String text = report.getExtractedText();
        if (text == null || text.isBlank()) {
            return false;
        }
        return tryRulesExtraction(reportId, text);
    }

    @Transactional
    public void restoreTextExtracted(Long reportId, String text) {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        report.markTextExtracted(text);
        labReportRepository.save(report);
    }

    @Transactional
    public void finalizeTextOnly(Long reportId) {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        requireReportDate(report);
        String text = report.getExtractedText();
        report.markTextOnly(text);
        labReportRepository.save(report);
        log.info("Report {} saved as text-only (CLAUDE_API_KEY not set)", reportId);
    }

    /**
     * Text extraction waterfall — AI is always last:
     * 1. PDF text layer (PDFBox)
     * 2. Local OCR (Tesseract) for scans/images
     * 3. Claude vision (optional, only if local methods fail or return no usable text)
     */
    private String extractTextFromSource(LabReport report) throws Exception {
        if (fileStorageService.hasReadableFile(report)) {
            Path filePath = Path.of(report.getStoragePath());
            String text = extractLocalText(report.getId(), filePath);
            if (text == null || text.isBlank()) {
                text = tryVisionTextFallback(report.getId(), filePath, text);
            }
            return text;
        }

        if (fileStorageService.canRetryFromStoredText(report)) {
            log.info("Report {} using stored extracted text (ingest file unavailable)", report.getId());
            String stored = report.getExtractedText();
            telemetry.textFromStored(report.getId(), stored);
            return stored;
        }

        return null;
    }

    private String extractLocalText(Long reportId, Path filePath) throws Exception {
        long start = System.currentTimeMillis();
        try {
            TextExtractionResult result = documentTextExtractor.extract(filePath);
            boolean lowQuality = result.method() != TextExtractionMethod.PDF_TEXT_LAYER
                    && textQualityAssessor.looksLowQualityOcr(result.text());
            if (lowQuality) {
                log.warn(
                        "Report {} OCR quality is low (method={}) — printed scans work best; handwriting may need review",
                        reportId,
                        result.method()
                );
            }
            telemetry.textExtracted(
                    reportId,
                    result.method(),
                    System.currentTimeMillis() - start,
                    result.text(),
                    lowQuality
            );
            log.info("Report {} local text extraction via {} ({} chars)", reportId, result.method(), result.text().length());
            return result.text();
        } catch (IOException | InterruptedException e) {
            log.warn("Report {} local text extraction failed: {}", reportId, e.getMessage());
            return tryVisionTextFallback(reportId, filePath, "");
        }
    }

    private String tryVisionTextFallback(Long reportId, Path filePath, String localText) throws Exception {
        if (!claudeVisionClient.isEnabled()) {
            if (localText != null && !localText.isBlank()) {
                return localText;
            }
            throw new IllegalStateException("Local text extraction failed and Claude vision fallback is disabled");
        }

        log.warn(
                "Report {} trying Claude vision as last-resort text extraction (local chars={})",
                reportId,
                localText == null ? 0 : localText.length()
        );
        long start = System.currentTimeMillis();
        ClaudeVisionTextResult vision = claudeVisionClient.extractText(filePath);
        telemetry.visionTextExtracted(
                reportId,
                System.currentTimeMillis() - start,
                vision.text(),
                vision.usage()
        );
        log.info("Report {} Claude vision extracted {} chars", reportId, vision.text().length());
        if (vision.text().length() > (localText == null ? 0 : localText.length())) {
            return vision.text();
        }
        return localText;
    }

    @Transactional
    public void completeExtraction(Long reportId, String text, List<ClaudeBiomarkerDto> extracted) {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        requireReportDate(report);

        biomarkerValueRepository.deleteByLabReportId(reportId);

        for (ClaudeBiomarkerDto dto : extracted) {
            String canonical = dto.canonical() != null && !dto.canonical().isBlank()
                    ? dto.canonical()
                    : normalizer.normalize(dto.testName());

            BiomarkerValue value = BiomarkerValue.fromExtraction(
                    report,
                    canonical,
                    dto.testName(),
                    dto.value(),
                    dto.textValue(),
                    dto.unit(),
                    dto.referenceRange(),
                    dto.confidence() != null ? BigDecimal.valueOf(dto.confidence()) : null,
                    dto.sourcePage()
            );
            biomarkerValueRepository.save(value);
        }

        report.markCompleted(text);
        labReportRepository.save(report);
        analyticsSyncService.syncReport(reportId);
        telemetry.pipelineCompleted(reportId, extracted.size());
        log.info("Report {} extraction completed with {} biomarkers", reportId, extracted.size());
    }

    @Transactional
    public void markFailed(Long reportId, String error) {
        labReportRepository.findById(reportId).ifPresent(report -> {
            report.markFailed(error);
            labReportRepository.save(report);
            telemetry.pipelineFailed(reportId, error);
        });
    }

    @Transactional(readOnly = true)
    public ReportDetail getReportDetail(Long reportId) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        List<BiomarkerValue> biomarkers = biomarkerValueRepository.findByLabReportIdOrderByCanonicalNameAsc(reportId);
        return new ReportDetail(report, biomarkers);
    }

    public record ReportDetail(LabReport report, List<BiomarkerValue> biomarkers) {
        public ExtractionStatus status() {
            return report.getExtractionStatus();
        }
    }
}
