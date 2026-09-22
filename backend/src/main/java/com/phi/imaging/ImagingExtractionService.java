package com.phi.imaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImagingExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ImagingExtractionService.class);

    private final LabReportRepository labReportRepository;
    private final ClaudeImagingClient claudeClient;
    private final ObjectMapper objectMapper;

    public ImagingExtractionService(
            LabReportRepository labReportRepository,
            ClaudeImagingClient claudeClient
    ) {
        this.labReportRepository = labReportRepository;
        this.claudeClient = claudeClient;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public void processAfterText(Long reportId) {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        if (!report.isImagingReport()) {
            return;
        }

        String text = report.getExtractedText();
        ImagingDtos.ExtractedImagingStudy study = ImagingTextParser.parse(text, report.getReportDate());

        boolean needsClaude = study.impression() == null
                || study.impression().isBlank()
                || study.findings().isEmpty();
        if (needsClaude && claudeClient.isConfigured()) {
            try {
                study = claudeClient.extractStudy(text, report.getReportDate());
                log.info("Report {} imaging parsed via Claude", reportId);
            } catch (Exception ex) {
                log.warn("Claude imaging parse failed for report {}: {}", reportId, ex.getMessage());
            }
        }

        report.markAwaitingImagingConfirmation(text, writeJson(study));
        labReportRepository.save(report);
        log.info("Report {} awaiting imaging confirmation", reportId);
    }

    @Transactional(readOnly = true)
    public ImagingDtos.ImagingDraftResponse draftForReport(Long reportId) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        return new ImagingDtos.ImagingDraftResponse(
                reportId,
                report.getReportDate() != null ? report.getReportDate().toString() : null,
                readStudy(report.getImagingExtract())
        );
    }

    public ImagingDtos.ExtractedImagingStudy readStudy(String json) {
        if (json == null || json.isBlank()) {
            return new ImagingDtos.ExtractedImagingStudy(
                    "OTHER", null, null, null, null, java.util.List.of()
            );
        }
        try {
            return objectMapper.readValue(json, ImagingDtos.ExtractedImagingStudy.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid imaging extract payload", ex);
        }
    }

    private String writeJson(ImagingDtos.ExtractedImagingStudy study) {
        try {
            return objectMapper.writeValueAsString(study);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize imaging study", ex);
        }
    }
}
