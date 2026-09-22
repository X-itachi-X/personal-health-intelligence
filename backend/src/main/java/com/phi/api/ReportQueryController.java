package com.phi.api;

import com.phi.access.AccessControlService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.BiomarkerValue;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.extraction.ReportExtractionService;
import com.phi.extraction.ReportExtractionService.ReportDetail;
import com.phi.imaging.ImagingExtractionService;
import com.phi.prescription.PrescriptionDtos;
import com.phi.prescription.PrescriptionExtractionService;
import com.phi.reasoning.ReportFindingsService;
import com.phi.storage.ReportFileStorageService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportQueryController {

    private final ReportExtractionService extractionService;
    private final LabReportRepository labReportRepository;
    private final AccessControlService accessControl;
    private final ReportFileStorageService fileStorageService;
    private final ReportFindingsService findingsService;
    private final PrescriptionExtractionService prescriptionExtractionService;
    private final ImagingExtractionService imagingExtractionService;

    public ReportQueryController(
            ReportExtractionService extractionService,
            LabReportRepository labReportRepository,
            AccessControlService accessControl,
            ReportFileStorageService fileStorageService,
            ReportFindingsService findingsService,
            PrescriptionExtractionService prescriptionExtractionService,
            ImagingExtractionService imagingExtractionService
    ) {
        this.extractionService = extractionService;
        this.labReportRepository = labReportRepository;
        this.accessControl = accessControl;
        this.fileStorageService = fileStorageService;
        this.findingsService = findingsService;
        this.prescriptionExtractionService = prescriptionExtractionService;
        this.imagingExtractionService = imagingExtractionService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listReports(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestParam(required = false) Long personId,
            @RequestParam(required = false) String familyId
    ) {
        accessControl.requireAuthenticated(account);

        List<LabReport> reports;
        if (personId != null) {
            if (!personId.equals(account.personId())) {
                accessControl.requireAdvanced(account);
                if (!accessControl.sameFamily(account.personId(), personId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
            }
            reports = labReportRepository.findActiveByPersonId(personId);
        } else if (familyId != null && accessControl.hasAdvancedAccess(account)) {
            accessControl.requireActiveMembership(familyId, account.personId());
            reports = labReportRepository.findActiveByFamilyId(familyId);
        } else {
            reports = labReportRepository.findActiveByPersonId(account.personId());
        }

        List<Map<String, Object>> body = reports.stream()
                .filter(report -> accessControl.canReadReport(account, report))
                .map(this::toSummaryMap)
                .toList();
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<Map<String, Object>> getReport(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId
    ) {
        ReportDetail detail = extractionService.getReportDetail(reportId);
        accessControl.requireCanReadReport(account, detail.report());

        List<Map<String, Object>> biomarkers = detail.biomarkers().stream()
                .map(this::toBiomarkerMap)
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reportId", detail.report().getId());
        body.put("filename", detail.report().getOriginalFilename());
        body.put("uploadedAt", detail.report().getUploadedAt().toString());
        body.put("reportDate", detail.report().getReportDate() != null
                ? detail.report().getReportDate().toString()
                : null);
        body.put("reportDateSource", detail.report().getReportDateSource() != null
                ? detail.report().getReportDateSource().name()
                : null);
        body.put("needsReportDate", detail.status() == com.phi.domain.ExtractionStatus.AWAITING_REPORT_DATE);
        body.put("canCorrectReportDate", detail.status() == com.phi.domain.ExtractionStatus.COMPLETED
                || detail.status() == com.phi.domain.ExtractionStatus.TEXT_ONLY
                || detail.report().getReportDateSource() == com.phi.domain.ReportDateSource.EXTRACTED);
        body.put("extractionStatus", detail.status().name());
        body.put("extractionError", detail.report().getExtractionError());
        body.put("personId", detail.report().getPerson().getId());
        body.put("familyId", detail.report().getFamilyId());
        body.put("biomarkerCount", biomarkers.size());
        body.put("biomarkers", biomarkers);
        String extractedText = detail.report().getExtractedText();
        body.put("extractedTextLength", extractedText != null ? extractedText.length() : 0);
        body.put("hasStoredText", fileStorageService.canRetryFromStoredText(detail.report()));
        body.put("hasReadableFile", fileStorageService.hasReadableFile(detail.report()));
        body.put("canRetry", detail.status() != com.phi.domain.ExtractionStatus.PROCESSING
                && (fileStorageService.hasReadableFile(detail.report())
                || fileStorageService.canRetryFromStoredText(detail.report())));
        body.put("documentType", detail.report().getDocumentType().name());
        body.put("needsMedicationConfirmation",
                detail.status() == com.phi.domain.ExtractionStatus.AWAITING_MEDICATION_CONFIRMATION);
        body.put("needsImagingConfirmation",
                detail.status() == com.phi.domain.ExtractionStatus.AWAITING_IMAGING_CONFIRMATION);
        if (detail.report().isPrescription()) {
            body.put("prescriptionItems", prescriptionExtractionService.itemsForReport(reportId).items());
        }
        if (detail.report().isImagingReport()) {
            body.put("imagingStudy", imagingExtractionService.draftForReport(reportId).study());
        }

        return ResponseEntity.ok(body);
    }

    @GetMapping("/{reportId}/imaging-draft")
    public com.phi.imaging.ImagingDtos.ImagingDraftResponse imagingDraft(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId
    ) {
        ReportDetail detail = extractionService.getReportDetail(reportId);
        accessControl.requireCanReadReport(account, detail.report());
        return imagingExtractionService.draftForReport(reportId);
    }

    @GetMapping("/{reportId}/prescription-items")
    public PrescriptionDtos.PrescriptionItemsResponse prescriptionItems(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId
    ) {
        ReportDetail detail = extractionService.getReportDetail(reportId);
        accessControl.requireCanReadReport(account, detail.report());
        return prescriptionExtractionService.itemsForReport(reportId);
    }

    @GetMapping("/{reportId}/findings")
    public ResponseEntity<ReportFindingsService.FindingsResponse> getFindings(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId
    ) {
        ReportDetail detail = extractionService.getReportDetail(reportId);
        accessControl.requireCanReadReport(account, detail.report());
        return ResponseEntity.ok(findingsService.findingsForReport(reportId));
    }

    private Map<String, Object> toSummaryMap(LabReport report) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("reportId", report.getId());
        map.put("filename", report.getOriginalFilename());
        map.put("uploadedAt", report.getUploadedAt().toString());
        map.put("reportDate", report.getReportDate() != null ? report.getReportDate().toString() : null);
        map.put("extractionStatus", report.getExtractionStatus().name());
        map.put("documentType", report.getDocumentType().name());
        map.put("personId", report.getPerson().getId());
        map.put("personName", report.getPerson().getDisplayName());
        return map;
    }

    private Map<String, Object> toBiomarkerMap(BiomarkerValue value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("canonical", value.getCanonicalName());
        map.put("testName", value.getRawTestName());
        map.put("value", value.getNumericValue());
        map.put("textValue", value.getTextValue());
        map.put("unit", value.getUnit());
        map.put("referenceRange", value.getReferenceRange());
        map.put("confidence", value.getConfidence());
        return map;
    }
}
