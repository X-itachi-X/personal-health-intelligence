import { colors } from "./theme";

export function formatEventType(eventType: string): string {
  const labels: Record<string, string> = {
    "pipeline.started": "Pipeline started",
    "text.pdf_layer": "PDF text layer",
    "text.ocr_pdf": "OCR (scanned PDF)",
    "text.ocr_image": "OCR (photo)",
    "text.plain": "Plain text",
    "text.ai_vision": "AI vision (last resort)",
    "text.stored_retry": "Stored text retry",
    "parse.rules": "Rules parse",
    "parse.claude": "Claude parse (last resort)",
    "parse.skipped": "Parse skipped",
    "file.purged": "File purged",
    "pipeline.completed": "Completed",
    "pipeline.failed": "Failed",
    "ops.free_tools": "Free-tool comparison",
  };
  return labels[eventType] ?? eventType;
}

export function eventStatusColor(status: string): string {
  switch (status) {
    case "success":
      return colors.success;
    case "warning":
      return colors.warning;
    case "error":
      return colors.danger;
    default:
      return colors.textMuted;
  }
}

export function formatTokenCount(n: number): string {
  if (n >= 1_000_000) {
    return `${(n / 1_000_000).toFixed(1)}M`;
  }
  if (n >= 1_000) {
    return `${(n / 1_000).toFixed(1)}k`;
  }
  return String(n);
}

export function formatDuration(ms: number | null): string | null {
  if (ms == null) return null;
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}

export function formatFileSize(bytes: number | null): string | null {
  if (bytes == null) return null;
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function isFreeToolEvent(eventType: string): boolean {
  return eventType !== "ops.free_tools"
    && !eventType.startsWith("parse.claude")
    && !eventType.startsWith("text.ai_vision");
}

export function formatAuditAction(action: string): string {
  return action.replace(/\./g, " · ").replace(/_/g, " ");
}
