package com.phi.extraction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClaudeResponseParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClaudeResponseParser() {
    }

    public List<ClaudeBiomarkerDto> parseBiomarkers(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = collectTextContent(root);
        String json = extractJsonArray(stripMarkdownFences(text));

        String stopReason = root.path("stop_reason").asText("");
        if ("max_tokens".equals(stopReason)) {
            throw new IllegalStateException(
                    "Claude response truncated before completing JSON array (max_tokens reached)");
        }

        return objectMapper.readValue(json, new TypeReference<List<ClaudeBiomarkerDto>>() {});
    }

    private static String collectTextContent(JsonNode root) {
        JsonNode content = root.path("content");
        if (!content.isArray() || content.isEmpty()) {
            throw new IllegalStateException("Claude response missing content");
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        if (text.isEmpty()) {
            throw new IllegalStateException("Claude response missing text content");
        }
        return text.toString();
    }

    private static String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstNewline = trimmed.indexOf('\n');
        if (firstNewline > 0) {
            trimmed = trimmed.substring(firstNewline + 1);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }
        return trimmed;
    }

    private static String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("No JSON array found in Claude response");
        }
        return text.substring(start, end + 1);
    }
}
