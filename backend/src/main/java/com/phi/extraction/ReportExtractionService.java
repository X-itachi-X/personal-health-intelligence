package com.phi.extraction;

import com.phi.domain.BiomarkerValue;
import com.phi.domain.BiomarkerValueRepository;
import com.phi.domain.ExtractionStatus;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ReportExtractionService.class);

    private final LabReportRepository labReportRepository;
    private final BiomarkerValueRepository biomarkerValueRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final ClaudeExtractionClient claudeClient;
    private final BiomarkerNormalizer normalizer;
    private final ReportExtractionService self;

    public ReportExtractionService(
            LabReportRepository labReportRepository,
            BiomarkerValueRepository biomarkerValueRepository,
            PdfTextExtractor pdfTextExtractor,
            ClaudeExtractionClient claudeClient,
            BiomarkerNormalizer normalizer,
            @Lazy ReportExtractionService self
    ) {
        this.labReportRepository = labReportRepository;
        this.biomarkerValueRepository = biomarkerValueRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.claudeClient = claudeClient;
        this.normalizer = normalizer;
        this.self = self;
    }

    @Async
    public void extractAsync(Long reportId) {
        try {
            String text = self.prepareExtraction(reportId);
            if (text == null) {
                return;
            }

            List<ClaudeBiomarkerDto> extracted = claudeClient.extractBiomarkers(text);
            self.completeExtraction(reportId, text, extracted);
        } catch (Exception e) {
            log.error("Extraction failed for report {}", reportId, e);
            self.markFailed(reportId, e.getMessage());
        }
    }

    @Transactional
    public String prepareExtraction(Long reportId) throws Exception {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

        report.markProcessing();
        labReportRepository.save(report);

        Path filePath = Path.of(report.getStoragePath());
        String text;

        if (pdfTextExtractor.isPdf(filePath)) {
            text = pdfTextExtractor.extractText(filePath);
        } else {
            text = "[Non-PDF file — image OCR not yet implemented]";
        }

        if (text.isBlank()) {
            report.markFailed("No text could be extracted from the document");
            labReportRepository.save(report);
            return null;
        }

        if (!claudeClient.isConfigured()) {
            report.markTextOnly(text);
            labReportRepository.save(report);
            log.info("Report {} saved with text only (CLAUDE_API_KEY not set)", reportId);
            return null;
        }

        return text;
    }

    @Transactional
    public void completeExtraction(Long reportId, String text, List<ClaudeBiomarkerDto> extracted) {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));

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
        log.info("Report {} extraction completed with {} biomarkers", reportId, extracted.size());
    }

    @Transactional
    public void markFailed(Long reportId, String error) {
        labReportRepository.findById(reportId).ifPresent(report -> {
            report.markFailed(error);
            labReportRepository.save(report);
        });
    }

    @Transactional(readOnly = true)
    public ReportDetail getReportDetail(Long reportId) {
        LabReport report = labReportRepository.findById(reportId)
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
