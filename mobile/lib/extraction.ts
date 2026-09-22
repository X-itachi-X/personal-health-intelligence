export type UploadPhase = "upload" | "extract" | "parse";

export function uploadPhaseFromStatus(status: string): UploadPhase {
  if (status === "TEXT_EXTRACTED" || status === "AWAITING_REPORT_DATE") {
    return "parse";
  }
  if (status === "PROCESSING" || status === "PENDING") {
    return "extract";
  }
  return "parse";
}

export function uploadPhaseLabel(phase: UploadPhase): string {
  switch (phase) {
    case "upload":
      return "Uploading file to server…";
    case "extract":
      return "Extracting text from PDF (local, no AI)…";
    case "parse":
      return "Parsing biomarkers with rules…";
  }
}

/** Short label for list badges and feed rows. */
export function extractionStatusBadgeLabel(status: string): string {
  switch (status) {
    case "PENDING":
      return "Queued";
    case "PROCESSING":
      return "Processing";
    case "TEXT_EXTRACTED":
      return "Ready to parse";
    case "AWAITING_REPORT_DATE":
      return "Needs date";
    case "AWAITING_MEDICATION_CONFIRMATION":
      return "Confirm meds";
    case "AWAITING_IMAGING_CONFIRMATION":
      return "Confirm scan";
    case "TEXT_ONLY":
      return "Text only";
    case "COMPLETED":
      return "Ready";
    case "FAILED":
      return "Failed";
    default:
      return status.replaceAll("_", " ").toLowerCase();
  }
}

export function extractionStatusVariant(
  status: string
): "success" | "warning" | "danger" | "default" {
  if (status === "COMPLETED") return "success";
  if (status === "FAILED") return "danger";
  if (
    status === "PROCESSING" ||
    status === "PENDING" ||
    status === "TEXT_EXTRACTED" ||
    status === "TEXT_ONLY" ||
    status.startsWith("AWAITING_")
  ) {
    return "warning";
  }
  return "default";
}

export function formatExtractionStatus(status: string): string {
  switch (status) {
    case "PENDING":
      return "Queued for extraction…";
    case "PROCESSING":
      return "Extracting text from your report…";
    case "TEXT_EXTRACTED":
      return "Text saved — parsing biomarkers…";
    case "AWAITING_REPORT_DATE":
      return "Report date required — when was this lab test done?";
    case "AWAITING_MEDICATION_CONFIRMATION":
      return "Review extracted medications and confirm";
    case "AWAITING_IMAGING_CONFIRMATION":
      return "Review extracted imaging findings and confirm";
    case "TEXT_ONLY":
      return "Text extracted — full parsing needs Claude on server";
    case "COMPLETED":
      return "Extraction complete";
    case "FAILED":
      return "Extraction failed";
    default:
      return status.replaceAll("_", " ").toLowerCase();
  }
}

export const FREE_TOOL_STEPS = [
  "PDF text layer (PDFBox)",
  "OCR all pages (Tesseract CLI)",
  "Rules parse per text source",
] as const;
