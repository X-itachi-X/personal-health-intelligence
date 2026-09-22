package com.phi.extraction;

/**
 * Result of running one free local text extraction tool (ops / debugging).
 */
public record FreeTextProbe(
        String toolId,
        String label,
        String status,
        Long durationMs,
        String text,
        String error,
        boolean usable,
        boolean lowQuality
) {
    public static FreeTextProbe skipped(String toolId, String label, String reason) {
        return new FreeTextProbe(toolId, label, "skipped", null, null, reason, false, false);
    }

    public static FreeTextProbe error(String toolId, String label, String error) {
        return new FreeTextProbe(toolId, label, "error", null, null, error, false, false);
    }
}
