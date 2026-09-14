package com.phi.extraction;

import com.phi.config.PhiProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClaudeExtractionClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String SYSTEM_PROMPT = """
            You extract laboratory test results from medical report text.
            Return ONLY a JSON array. No markdown, no explanation.
            Each object must have:
            - testName: original test name from the report
            - canonical: snake_case biomarker id (e.g. vitamin_d, alt, hba1c)
            - value: numeric value if parseable, else null
            - textValue: string value if non-numeric (e.g. "Negative"), else null
            - unit: measurement unit
            - referenceRange: lab reference range as printed
            - confidence: 0.0 to 1.0 extraction confidence
            - sourcePage: page number if known, else null
            Extract every test result you can find. Do not invent values not present in the text.
            """;

    private final PhiProperties properties;
    private final ClaudeResponseParser parser;
    private final HttpClient httpClient;

    public ClaudeExtractionClient(PhiProperties properties, ClaudeResponseParser parser) {
        this.properties = properties;
        this.parser = parser;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public boolean isConfigured() {
        String key = properties.claude().apiKey();
        return key != null && !key.isBlank();
    }

    public List<ClaudeBiomarkerDto> extractBiomarkers(String reportText) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("CLAUDE_API_KEY is not configured");
        }

        String userPrompt = "Extract all lab test results from this report text:\n\n" + truncate(reportText);
        String requestBody = buildRequestBody(userPrompt);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "application/json")
                .header("x-api-key", properties.claude().apiKey())
                .header("anthropic-version", "2023-06-01");

        String workspaceId = properties.claude().workspaceId();
        if (workspaceId != null && !workspaceId.isBlank()) {
            requestBuilder.header("anthropic-workspace-id", workspaceId);
        }

        HttpRequest request = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Claude API error " + response.statusCode() + ": " + response.body());
        }

        return parser.parseBiomarkers(response.body());
    }

    private String buildRequestBody(String userPrompt) {
        return """
                {
                  "model": "%s",
                  "max_tokens": 16384,
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

    private static String truncate(String text) {
        if (text.length() <= 50000) {
            return text;
        }
        return text.substring(0, 50000);
    }

    private static String jsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "") + "\"";
    }
}
