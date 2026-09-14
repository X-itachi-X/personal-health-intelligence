package com.phi.api;

import com.phi.domain.BiomarkerValue;
import com.phi.extraction.ReportExtractionService;
import com.phi.extraction.ReportExtractionService.ReportDetail;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportQueryController {

    private final ReportExtractionService extractionService;

    public ReportQueryController(ReportExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<Map<String, Object>> getReport(@PathVariable Long reportId) {
        ReportDetail detail = extractionService.getReportDetail(reportId);

        List<Map<String, Object>> biomarkers = detail.biomarkers().stream()
                .map(this::toBiomarkerMap)
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reportId", detail.report().getId());
        body.put("filename", detail.report().getOriginalFilename());
        body.put("uploadedAt", detail.report().getUploadedAt().toString());
        body.put("extractionStatus", detail.status().name());
        body.put("extractionError", detail.report().getExtractionError());
        body.put("biomarkerCount", biomarkers.size());
        body.put("biomarkers", biomarkers);

        return ResponseEntity.ok(body);
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
