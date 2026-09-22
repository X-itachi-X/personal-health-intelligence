package com.phi.prescription;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phi.config.PhiProperties;
import com.phi.domain.MedicationCourseType;
import com.phi.extraction.ClaudeUsageParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClaudePrescriptionClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String SYSTEM_PROMPT = """
            You extract medication lines from prescription text.
            Return ONLY a JSON array. No markdown, no explanation.
            Each object must have:
            - medicationName: drug name with strength if present
            - dosage: e.g. "1 tablet" if stated, else null
            - scheduleText: timings such as "1-0-1 after food", "BD", "OD", else null
            - durationDays: integer days if a short course is stated, else null
            - courseType: ACUTE, CHRONIC, or UNKNOWN
            ACUTE = antibiotics/antimalarials/short courses. CHRONIC = diabetes/BP/thyroid/long-term meds.
            Do not invent medications not present in the text.
            """;

    private final PhiProperties properties;
    private final ClaudeUsageParser usageParser;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ClaudePrescriptionClient(PhiProperties properties, ClaudeUsageParser usageParser) {
        this.properties = properties;
        this.usageParser = usageParser;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    }

    public boolean isConfigured() {
        String key = properties.claude().apiKey();
        return key != null && !key.isBlank();
    }

    public List<PrescriptionDtos.ExtractedMedicationItem> extractMedications(String text, LocalDate defaultStartDate)
            throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("CLAUDE_API_KEY is not configured");
        }

        String model = properties.claude().model();
        String userPrompt = "Extract all medications from this prescription text:\n\n" + truncate(text);
        String requestBody = buildRequestBody(userPrompt);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "application/json")
                .header("x-api-key", properties.claude().apiKey())
                .header("anthropic-version", "2023-06-01");

        HttpResponse<String> response = httpClient.send(
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build(),
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Claude prescription parse failed: HTTP " + response.statusCode());
        }

        usageParser.parse(response.body(), model);
        return parseItems(response.body(), defaultStartDate);
    }

    private List<PrescriptionDtos.ExtractedMedicationItem> parseItems(String responseBody, LocalDate defaultStartDate)
            throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        StringBuilder text = new StringBuilder();
        for (JsonNode block : root.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        String json = text.toString().trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("^```[a-z]*\\n?", "").replaceAll("\\n?```$", "").trim();
        }
        int start = json.indexOf('[');
        int end = json.lastIndexOf(']');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }

        List<RawItem> rawItems = objectMapper.readValue(json, new TypeReference<List<RawItem>>() {});
        return rawItems.stream()
                .filter(item -> item.medicationName != null && !item.medicationName.isBlank())
                .map(item -> new PrescriptionDtos.ExtractedMedicationItem(
                        item.medicationName.trim(),
                        blankToNull(item.dosage),
                        blankToNull(item.scheduleText),
                        defaultStartDate,
                        item.durationDays,
                        parseCourseType(item.courseType)
                ))
                .toList();
    }

    private static MedicationCourseType parseCourseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return MedicationCourseType.UNKNOWN;
        }
        try {
            return MedicationCourseType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MedicationCourseType.UNKNOWN;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String buildRequestBody(String userPrompt) {
        return """
                {
                  "model": "%s",
                  "max_tokens": 4096,
                  "system": %s,
                  "messages": [
                    {"role": "user", "content": %s}
                  ]
                }
                """.formatted(
                properties.claude().model(),
                jsonString(SYSTEM_PROMPT),
                jsonString(userPrompt)
        );
    }

    private static String jsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "") + "\"";
    }

    private static String truncate(String text) {
        return text.length() <= 120_000 ? text : text.substring(0, 120_000);
    }

    private static final class RawItem {
        public String medicationName;
        public String dosage;
        public String scheduleText;
        public Integer durationDays;
        public String courseType;
    }
}
