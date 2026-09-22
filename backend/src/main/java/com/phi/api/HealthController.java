package com.phi.api;

import com.phi.config.PhiProperties;
import com.phi.extraction.ClaudeExtractionClient;
import com.phi.extraction.OcrTextExtractor;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final ClaudeExtractionClient claudeClient;
    private final OcrTextExtractor ocrTextExtractor;
    private final PhiProperties properties;

    public HealthController(
            ClaudeExtractionClient claudeClient,
            OcrTextExtractor ocrTextExtractor,
            PhiProperties properties
    ) {
        this.claudeClient = claudeClient;
        this.ocrTextExtractor = ocrTextExtractor;
        this.properties = properties;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("service", "personal-health-intelligence");
        body.put("claudeConfigured", claudeClient.isConfigured());
        body.put("claudeVisionFallback", properties.claude().visionFallbackEnabled());
        body.put("claudeModel", properties.claude().model());
        String workspaceId = properties.claude().workspaceId();
        body.put("claudeWorkspaceConfigured", workspaceId != null && !workspaceId.isBlank());
        body.put("ocrEnabled", properties.ocr().enabled());
        body.put("ocrAvailable", ocrTextExtractor.isAvailable());
        body.put("ocrLanguages", properties.ocr().languages());
        return ResponseEntity.ok(body);
    }
}
