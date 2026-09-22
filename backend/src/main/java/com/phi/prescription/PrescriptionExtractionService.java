package com.phi.prescription;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrescriptionExtractionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionExtractionService.class);

    private final LabReportRepository labReportRepository;
    private final ClaudePrescriptionClient claudeClient;
    private final ObjectMapper objectMapper;

    public PrescriptionExtractionService(
            LabReportRepository labReportRepository,
            ClaudePrescriptionClient claudeClient
    ) {
        this.labReportRepository = labReportRepository;
        this.claudeClient = claudeClient;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public void processAfterText(Long reportId) {
        LabReport report = labReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        if (!report.isPrescription()) {
            return;
        }

        String text = report.getExtractedText();
        List<PrescriptionDtos.ExtractedMedicationItem> items = PrescriptionTextParser.parse(
                text,
                report.getReportDate()
        );

        if (items.isEmpty() && claudeClient.isConfigured()) {
            try {
                items = claudeClient.extractMedications(text, report.getReportDate());
                log.info("Report {} prescription parsed via Claude ({} items)", reportId, items.size());
            } catch (Exception ex) {
                log.warn("Claude prescription parse failed for report {}: {}", reportId, ex.getMessage());
            }
        }

        String json = writeJson(items);
        report.markAwaitingMedicationConfirmation(text, json);
        labReportRepository.save(report);
        log.info("Report {} awaiting medication confirmation ({} items)", reportId, items.size());
    }

    @Transactional(readOnly = true)
    public PrescriptionDtos.PrescriptionItemsResponse itemsForReport(Long reportId) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
        return new PrescriptionDtos.PrescriptionItemsResponse(
                reportId,
                report.getReportDate() != null ? report.getReportDate().toString() : null,
                readItems(report.getPrescriptionExtract())
        );
    }

    public List<PrescriptionDtos.ExtractedMedicationItem> readItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<PrescriptionDtos.ExtractedMedicationItem>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid prescription extract payload", ex);
        }
    }

    private String writeJson(List<PrescriptionDtos.ExtractedMedicationItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize prescription items", ex);
        }
    }
}
