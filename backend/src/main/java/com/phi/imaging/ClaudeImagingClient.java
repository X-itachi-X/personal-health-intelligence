package com.phi.imaging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import com.phi.config.PhiProperties;
import com.phi.extraction.ClaudeUsageParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class ClaudeImagingClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String SYSTEM_PROMPT = """
            You extract structured data from radiology report text (X-ray, ultrasound/USG, CT, MRI).
            Return ONLY a JSON object. No markdown, no explanation.
            Fields:
            - modality: XRAY, ULTRASOUND, CT, MRI, or OTHER
            - bodyRegion: e.g. abdomen, chest, brain
            - facility: hospital/clinic name if present, else null
            - impression: short summary paragraph
            - findings: array of {findingText, severity, measurementValue, measurementUnit}
            severity: info, low, medium, or high
            Do not invent findings not present in the text.
            """;

    private final PhiProperties properties;
    private final ClaudeUsageParser usageParser;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public ClaudeImagingClient(PhiProperties properties, ClaudeUsageParser usageParser) {
        this.properties = properties;
        this.usageParser = usageParser;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    }

    public boolean isConfigured() {
        String key = properties.claude().apiKey();
        return key != null && !key.isBlank();
    }

    public ImagingDtos.ExtractedImagingStudy extractStudy(String text, LocalDate defaultStudyDate) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("CLAUDE_API_KEY is not configured");
        }

        String model = properties.claude().model();
        String userPrompt = "Extract imaging study data from this radiology report:\n\n" + truncate(text);
        String requestBody = buildRequestBody(userPrompt);

        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .timeout(Duration.ofMinutes(5))
                        .header("Content-Type", "application/json")
                        .header("x-api-key", properties.claude().apiKey())
                        .header("anthropic-version", "2023-06-01")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Claude imaging parse failed: HTTP " + response.statusCode());
        }

        usageParser.parse(response.body(), model);
        return parseStudy(response.body(), defaultStudyDate);
    }

    private ImagingDtos.ExtractedImagingStudy parseStudy(String responseBody, LocalDate defaultStudyDate) throws Exception {
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
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }

        JsonNode study = objectMapper.readTree(json);
        List<ImagingDtos.ExtractedFinding> findings = objectMapper.convertValue(
                study.path("findings"),
                new TypeReference<List<ImagingDtos.ExtractedFinding>>() {}
        );
        return new ImagingDtos.ExtractedImagingStudy(
                study.path("modality").asText("OTHER"),
                blankToNull(study.path("bodyRegion").asText(null)),
                defaultStudyDate,
                blankToNull(study.path("facility").asText(null)),
                blankToNull(study.path("impression").asText(null)),
                findings
        );
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
}
