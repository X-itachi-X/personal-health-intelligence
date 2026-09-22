package com.phi.api;

import com.phi.access.AccessControlService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.DocumentType;
import com.phi.domain.ExtractionStatus;
import com.phi.domain.LabReport;
import com.phi.imaging.ImagingConfirmService;
import com.phi.imaging.ImagingDtos;
import com.phi.prescription.PrescriptionConfirmService;
import com.phi.prescription.PrescriptionDtos;
import com.phi.domain.LabReportRepository;
import com.phi.extraction.ReportExtractionService;
import com.phi.ingestion.ReportIngestionService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportIngestionService ingestionService;
    private final ReportExtractionService extractionService;
    private final LabReportRepository labReportRepository;
    private final AccessControlService accessControl;
    private final PrescriptionConfirmService prescriptionConfirmService;
    private final ImagingConfirmService imagingConfirmService;

    public ReportController(
            ReportIngestionService ingestionService,
            ReportExtractionService extractionService,
            LabReportRepository labReportRepository,
            AccessControlService accessControl,
            PrescriptionConfirmService prescriptionConfirmService,
            ImagingConfirmService imagingConfirmService
    ) {
        this.ingestionService = ingestionService;
        this.extractionService = extractionService;
        this.labReportRepository = labReportRepository;
        this.accessControl = accessControl;
        this.prescriptionConfirmService = prescriptionConfirmService;
        this.imagingConfirmService = imagingConfirmService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String familyId,
            @RequestParam(required = false) Long personId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @RequestParam(required = false, defaultValue = "LAB_REPORT") DocumentType documentType
    ) throws IOException {
        LabReport report = ingestionService.ingest(file, account, familyId, personId, reportDate, documentType);
        extractionService.extractAsync(report.getId());

        Map<String, Object> body = reportResponse(report, "Report received. Extraction started.");
        return ResponseEntity.ok(body);
    }

    @PostMapping(path = "/{reportId}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> replaceFailedReport(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate
    ) throws IOException {
        LabReport report = ingestionService.replaceFailedReport(file, account, reportId, reportDate);
        extractionService.extractAsync(report.getId());

        Map<String, Object> body = reportResponse(report, "Failed report replaced. Extraction started.");
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{reportId}/report-date")
    public ResponseEntity<Map<String, Object>> submitReportDate(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId,
            @RequestBody Map<String, String> body
    ) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        accessControl.requireCanReadReport(account, report);

        String rawDate = body.get("reportDate");
        if (rawDate == null || rawDate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reportDate is required (YYYY-MM-DD)");
        }

        boolean correct = Boolean.parseBoolean(String.valueOf(body.getOrDefault("correct", "false")));

        try {
            LocalDate reportDate = LocalDate.parse(rawDate);
            if (correct) {
                extractionService.correctReportDateAndContinue(reportId, reportDate);
            } else {
                extractionService.submitReportDateAndContinue(reportId, reportDate);
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("reportId", reportId);
        response.put("reportDate", rawDate);
        response.put("extractionStatus", ExtractionStatus.PROCESSING.name());
        response.put("message", "Report date saved. Extraction continuing.");
        return ResponseEntity.accepted().body(response);
    }

    @PostMapping("/{reportId}/retry")
    public ResponseEntity<Map<String, Object>> retryExtraction(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId
    ) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        accessControl.requireCanReadReport(account, report);

        try {
            extractionService.retryExtraction(reportId);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reportId", reportId);
        body.put("extractionStatus", ExtractionStatus.PROCESSING.name());
        body.put("message", "Extraction retry started");
        return ResponseEntity.accepted().body(body);
    }

    @PostMapping("/{reportId}/parse")
    public ResponseEntity<Map<String, Object>> parseWithClaude(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId
    ) {
        LabReport report = labReportRepository.findActiveById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        accessControl.requireCanReadReport(account, report);

        try {
            extractionService.parseWithClaudeAsync(reportId);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reportId", reportId);
        body.put("extractionStatus", ExtractionStatus.PROCESSING.name());
        body.put("message", "Claude parsing started");
        return ResponseEntity.accepted().body(body);
    }

    @PostMapping("/{reportId}/confirm-imaging")
    public ImagingDtos.ConfirmImagingResponse confirmImaging(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId,
            @RequestBody ImagingDtos.ConfirmImagingRequest request
    ) {
        return imagingConfirmService.confirm(account, reportId, request);
    }

    @PostMapping("/{reportId}/confirm-medications")
    public PrescriptionDtos.ConfirmMedicationsResponse confirmMedications(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId,
            @RequestBody PrescriptionDtos.ConfirmMedicationsRequest request
    ) {
        return prescriptionConfirmService.confirm(account, reportId, request);
    }

    @DeleteMapping("/{reportId}")
    public ResponseEntity<Map<String, Object>> delete(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long reportId
    ) {
        ingestionService.softDelete(account, reportId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reportId", reportId);
        body.put("deleted", true);
        return ResponseEntity.ok(body);
    }

    private static Map<String, Object> reportResponse(LabReport report, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reportId", report.getId());
        body.put("filename", report.getOriginalFilename());
        body.put("uploadedAt", report.getUploadedAt().toString());
        body.put("reportDate", report.getReportDate() != null ? report.getReportDate().toString() : null);
        body.put("extractionStatus", report.getExtractionStatus().name());
        body.put("personId", report.getPerson().getId());
        body.put("familyId", report.getFamilyId());
        body.put("documentType", report.getDocumentType().name());
        body.put("message", message);
        return body;
    }
}
