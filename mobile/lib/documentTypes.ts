import { DocumentType, ReportDetail } from "./api";

export function uploadSheetTitle(documentType: DocumentType = "LAB_REPORT"): string {
  switch (documentType) {
    case "PRESCRIPTION":
      return "Upload prescription";
    case "IMAGING_REPORT":
      return "Upload scan report";
    default:
      return "Upload lab report";
  }
}

export function parseStepLabel(documentType: DocumentType = "LAB_REPORT"): string {
  switch (documentType) {
    case "PRESCRIPTION":
      return "Extract medications";
    case "IMAGING_REPORT":
      return "Extract findings";
    default:
      return "Parse biomarkers (rules)";
  }
}

export function photoDateRequiredHint(documentType: DocumentType = "LAB_REPORT"): string {
  switch (documentType) {
    case "PRESCRIPTION":
      return "Required for photos — enter the prescription date.";
    case "IMAGING_REPORT":
      return "Required for photos — enter when the scan was done.";
    default:
      return "Required for photos — we do not auto-detect dates from camera scans.";
  }
}

export function awaitingDatePhotoMessage(
  documentType: DocumentType = "LAB_REPORT",
  isPhoto = false
): string {
  if (!isPhoto) {
    return documentType === "IMAGING_REPORT"
      ? "We could not read a reliable scan date. Enter when the study was done."
      : documentType === "PRESCRIPTION"
        ? "We could not read a reliable prescription date. Enter when it was written."
        : "We could not read a reliable report date. Enter when the lab test was done.";
  }
  switch (documentType) {
    case "PRESCRIPTION":
      return "Photo uploads need the prescription date — we do not guess dates from scanned images.";
    case "IMAGING_REPORT":
      return "Photo uploads need the scan date — we do not guess dates from scanned images.";
    default:
      return "Photo uploads need the lab test date — we do not guess dates from scanned images.";
  }
}

export function reportDateRequiredCaption(documentType: DocumentType = "LAB_REPORT"): string {
  switch (documentType) {
    case "PRESCRIPTION":
      return "Medication timelines need the prescription date. Upload date is never used on dashboards.";
    case "IMAGING_REPORT":
      return "Imaging timelines need the study date. Upload date is never used on dashboards.";
    default:
      return "Trends and insights need the actual lab test date. Upload date is never used on dashboards.";
  }
}

export function reportDatePickerLabel(documentType: DocumentType = "LAB_REPORT"): string {
  switch (documentType) {
    case "PRESCRIPTION":
      return "When was this prescription written?";
    case "IMAGING_REPORT":
      return "When was this scan done?";
    default:
      return "When was this test done?";
  }
}

export function finishUploadMessage(report: Pick<ReportDetail, "documentType" | "extractionStatus" | "biomarkerCount">): string {
  const docType = report.documentType ?? "LAB_REPORT";
  if (report.extractionStatus === "TEXT_ONLY") {
    return "Text extracted. Full parsing needs Claude API key on server.";
  }
  if (docType === "PRESCRIPTION" && report.extractionStatus === "COMPLETED") {
    return "Prescription processed — medications are in your log.";
  }
  if (docType === "IMAGING_REPORT" && report.extractionStatus === "COMPLETED") {
    return "Scan report saved — see imaging timeline on Trends.";
  }
  return `Done — ${report.biomarkerCount} biomarkers found.`;
}

export function documentTypeBadgeLabel(documentType: DocumentType = "LAB_REPORT"): string {
  switch (documentType) {
    case "PRESCRIPTION":
      return "Prescription";
    case "IMAGING_REPORT":
      return "Imaging";
    default:
      return "Lab report";
  }
}
