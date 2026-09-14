package com.phi.api;

import com.phi.config.PhiProperties;
import com.phi.extraction.ClaudeExtractionClient;
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
    private final PhiProperties properties;

    public HealthController(ClaudeExtractionClient claudeClient, PhiProperties properties) {
        this.claudeClient = claudeClient;
        this.properties = properties;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "ok");
        body.put("service", "personal-health-intelligence");
        body.put("claudeConfigured", claudeClient.isConfigured());
        body.put("claudeModel", properties.claude().model());
        String workspaceId = properties.claude().workspaceId();
        body.put("claudeWorkspaceConfigured", workspaceId != null && !workspaceId.isBlank());
        return ResponseEntity.ok(body);
    }
}
