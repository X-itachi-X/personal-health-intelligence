package com.phi.telemetry;

final class TelemetryMetadata {

    private static final int PREVIEW_MAX_CHARS = 4000;

    private TelemetryMetadata() {
    }

    static String withTextPreview(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String preview = text.length() > PREVIEW_MAX_CHARS
                ? text.substring(0, PREVIEW_MAX_CHARS) + "…"
                : text;
        return "{\"preview\":\"" + escapeJson(preview) + "\"}";
    }

    static String withTextPreviewAndFlag(String text, String flagKey, boolean flagValue) {
        String base = withTextPreview(text);
        if (base == null) {
            return "{\"" + flagKey + "\":" + flagValue + "}";
        }
        return base.substring(0, base.length() - 1) + ",\"" + flagKey + "\":" + flagValue + "}";
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }
}
