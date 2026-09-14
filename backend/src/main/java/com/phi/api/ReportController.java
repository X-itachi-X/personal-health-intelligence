package com.phi.api;

import com.phi.domain.LabReport;
import com.phi.extraction.ReportExtractionService;
import com.phi.ingestion.ReportIngestionService;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportIngestionService ingestionService;
    private final ReportExtractionService extractionService;

    public ReportController(
            ReportIngestionService ingestionService,
            ReportExtractionService extractionService
    ) {
        this.ingestionService = ingestionService;
        this.extractionService = extractionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        LabReport report = ingestionService.ingest(file);
        extractionService.extractAsync(report.getId());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reportId", report.getId());
        body.put("filename", report.getOriginalFilename());
        body.put("uploadedAt", report.getUploadedAt().toString());
        body.put("extractionStatus", report.getExtractionStatus().name());
        body.put("message", "Report received. Extraction started.");

        return ResponseEntity.ok(body);
    }
}
