package com.phi.config;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "phi")
public record PhiProperties(
        Storage storage,
        Claude claude,
        Ocr ocr,
        Jwt jwt,
        Google google,
        Analytics analytics,
        Platform platform
) {

    public record Storage(String reportsDir, int retainFilesDays) {
        public int effectiveRetainFilesDays() {
            return Math.max(retainFilesDays, 0);
        }
    }

    public record Analytics(String duckdbPath) {
    }

    public record Ocr(
            boolean enabled,
            String languages,
            String tessdataPath,
            int minTextLayerChars,
            int minLetterChars,
            int minOcrChars,
            int renderDpi,
            int pageSegMode,
            int maxPages
    ) {
        public int effectiveRenderDpi() {
            return renderDpi > 0 ? renderDpi : 300;
        }

        /** 0 = no page limit. */
        public int effectiveMaxPages() {
            return Math.max(maxPages, 0);
        }

        public String resolvedTessdataPath() {
            if (tessdataPath != null && !tessdataPath.isBlank()) {
                return tessdataPath;
            }
            String env = System.getenv("TESSDATA_PREFIX");
            if (env != null && !env.isBlank()) {
                return env;
            }
            return "/usr/share/tesseract/tessdata";
        }
    }

    public record Claude(
            String apiKey,
            String model,
            String workspaceId,
            boolean autoParse,
            double minCoverage,
            boolean visionFallback
    ) {
        public boolean autoParseEnabled() {
            return autoParse;
        }

        public boolean visionFallbackEnabled() {
            return visionFallback;
        }

        public double effectiveMinCoverage() {
            if (minCoverage <= 0) {
                return 0.8;
            }
            return Math.min(minCoverage, 1.0);
        }
    }

    public record Jwt(String secret, long expirationHours) {
    }

    public record Google(String clientId) {
    }

    public record Platform(String adminEmails) {
        public Set<String> adminEmailSet() {
            if (adminEmails == null || adminEmails.isBlank()) {
                return Set.of();
            }
            return Stream.of(adminEmails.split(","))
                    .map(String::trim)
                    .filter(email -> !email.isEmpty())
                    .map(email -> email.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
        }
    }
}
