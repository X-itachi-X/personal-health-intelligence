package com.phi.extraction;

import com.phi.config.PhiProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

/**
 * Last-resort text extraction via Claude vision — only when local PDF/OCR methods fail.
 */
@Component
public class ClaudeVisionTextClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final int MAX_PAGES = 10;
    private static final String PROMPT = """
            Extract every piece of text visible in this lab report image.
            Return only the raw text as printed, preserving line breaks.
            No markdown, no commentary, no invented values.
            """;

    private final PhiProperties properties;
    private final PdfTextExtractor pdfTextExtractor;
    private final ClaudeUsageParser usageParser;
    private final HttpClient httpClient;

    public ClaudeVisionTextClient(
            PhiProperties properties,
            PdfTextExtractor pdfTextExtractor,
            ClaudeUsageParser usageParser
    ) {
        this.properties = properties;
        this.pdfTextExtractor = pdfTextExtractor;
        this.usageParser = usageParser;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public boolean isConfigured() {
        String key = properties.claude().apiKey();
        return key != null && !key.isBlank();
    }

    public boolean isEnabled() {
        return properties.claude().visionFallbackEnabled() && isConfigured();
    }

    public ClaudeVisionTextResult extractText(Path filePath) throws Exception {
        if (!isEnabled()) {
            throw new IllegalStateException("Claude vision fallback is not enabled");
        }

        String model = properties.claude().model();
        List<ImagePayload> images = loadImages(filePath);
        if (images.isEmpty()) {
            throw new IllegalStateException("No images to send for vision extraction");
        }

        String requestBody = buildRequestBody(images);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofMinutes(5))
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
            throw new IllegalStateException("Claude vision API error " + response.statusCode() + ": " + response.body());
        }

        String body = response.body();
        String text = parseTextResponse(body).trim();
        ClaudeApiUsage usage = usageParser.parse(body, model);
        return new ClaudeVisionTextResult(text, usage);
    }

    private List<ImagePayload> loadImages(Path filePath) throws IOException {
        if (pdfTextExtractor.isPdf(filePath)) {
            return renderPdfPages(filePath);
        }
        String mediaType = mediaTypeFor(filePath);
        if (mediaType == null) {
            throw new IOException("Unsupported file type for vision: " + filePath.getFileName());
        }
        byte[] bytes = Files.readAllBytes(filePath);
        return List.of(new ImagePayload(mediaType, Base64.getEncoder().encodeToString(bytes)));
    }

    private List<ImagePayload> renderPdfPages(Path pdfPath) throws IOException {
        List<ImagePayload> images = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            int dpi = properties.ocr().effectiveRenderDpi();
            int pageCount = Math.min(document.getNumberOfPages(), MAX_PAGES);
            for (int page = 0; page < pageCount; page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, dpi);
                images.add(new ImagePayload("image/jpeg", encodeJpeg(image)));
            }
        }
        return images;
    }

    private static String encodeJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", output);
        return Base64.getEncoder().encodeToString(output.toByteArray());
    }

    private static String mediaTypeFor(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        if (name.endsWith(".gif")) {
            return "image/gif";
        }
        return null;
    }

    private String buildRequestBody(List<ImagePayload> images) {
        StringBuilder content = new StringBuilder("[");
        for (int i = 0; i < images.size(); i++) {
            ImagePayload image = images.get(i);
            if (i > 0) {
                content.append(',');
            }
            content.append("""
                    {"type":"image","source":{"type":"base64","media_type":"%s","data":"%s"}}
                    """.formatted(image.mediaType(), image.base64Data()));
        }
        content.append(",{\"type\":\"text\",\"text\":").append(jsonString(PROMPT)).append('}');
        content.append(']');

        return """
                {
                  "model": "%s",
                  "max_tokens": 16384,
                  "messages": [
                    {"role": "user", "content": %s}
                  ]
                }
                """.formatted(properties.claude().model(), content);
    }

    private static String parseTextResponse(String responseBody) {
        int textIndex = responseBody.indexOf("\"text\":");
        if (textIndex < 0) {
            throw new IllegalStateException("Claude vision response missing text content");
        }
        int start = responseBody.indexOf('"', textIndex + 7) + 1;
        StringBuilder text = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < responseBody.length(); i++) {
            char c = responseBody.charAt(i);
            if (escaped) {
                if (c == 'n') {
                    text.append('\n');
                } else if (c == 'r') {
                    text.append('\r');
                } else if (c == 't') {
                    text.append('\t');
                } else {
                    text.append(c);
                }
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                break;
            }
            text.append(c);
        }
        return text.toString();
    }

    private static String jsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "") + "\"";
    }

    private record ImagePayload(String mediaType, String base64Data) {
    }
}
