import { Biomarker } from "./api";
import { colors } from "./theme";

export type RangeStatus = "normal" | "low" | "high" | "unknown";

export type BiomarkerAssessment = {
  status: RangeStatus;
  label: string;
};

export function formatBiomarkerName(canonical: string): string {
  return canonical
    .replace(/_/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

export function parseReferenceRange(range: string): { min?: number; max?: number } | null {
  const cleaned = range.trim().replace(/\s+/g, "");
  if (!cleaned) return null;

  const between = cleaned.match(/^([\d.]+)\s*[-–]\s*([\d.]+)$/);
  if (between) {
    return { min: Number(between[1]), max: Number(between[2]) };
  }

  const lessThan = cleaned.match(/^<?=?([\d.]+)$/);
  if (lessThan) {
    return { max: Number(lessThan[1]) };
  }

  const greaterThan = cleaned.match(/^>=?=?([\d.]+)$/);
  if (greaterThan) {
    return { min: Number(greaterThan[1]) };
  }

  return null;
}

export function assessBiomarker(biomarker: Biomarker): BiomarkerAssessment {
  const name = formatBiomarkerName(biomarker.canonical);
  const numeric = biomarker.value;

  if (numeric == null || !biomarker.referenceRange) {
    return { status: "unknown", label: name };
  }

  const bounds = parseReferenceRange(biomarker.referenceRange);
  if (!bounds) {
    return { status: "unknown", label: name };
  }

  if (bounds.min != null && numeric < bounds.min) {
    return { status: "low", label: name };
  }
  if (bounds.max != null && numeric > bounds.max) {
    return { status: "high", label: name };
  }

  return { status: "normal", label: name };
}

export function statusColor(status: RangeStatus): string {
  switch (status) {
    case "high":
    case "low":
      return colors.danger;
    case "normal":
      return colors.success;
    default:
      return colors.textMuted;
  }
}

export function statusText(status: RangeStatus): string {
  switch (status) {
    case "high":
      return "Above range";
    case "low":
      return "Below range";
    case "normal":
      return "In range";
    default:
      return "";
  }
}

export function buildHealthSummary(biomarkers: Biomarker[]): string {
  if (biomarkers.length === 0) {
    return "Upload a lab report to get a plain-language summary of your results.";
  }

  const assessed = biomarkers.map((b) => ({ biomarker: b, ...assessBiomarker(b) }));
  const flagged = assessed.filter((a) => a.status === "high" || a.status === "low");
  const normal = assessed.filter((a) => a.status === "normal");

  if (flagged.length === 0 && normal.length > 0) {
    return `Good news — all ${normal.length} checked values in your latest report are within the usual ranges.`;
  }

  if (flagged.length === 0) {
    return "Your latest report is ready. Open it from Reports to review the details.";
  }

  if (flagged.length === 1) {
    const item = flagged[0];
    const direction = item.status === "high" ? "above" : "below";
    return `Your ${item.label} is ${direction} the usual range. It's worth mentioning to your doctor at your next visit.`;
  }

  if (flagged.length <= 3) {
    const names = flagged.map((f) => f.label).join(", ");
    return `${flagged.length} results need attention: ${names}. Review the full report and discuss with your doctor.`;
  }

  return `${flagged.length} results are outside the usual ranges. Review your full report and follow up with your doctor.`;
}

export function summarizeFlagged(biomarkers: Biomarker[]): BiomarkerAssessment[] {
  return biomarkers
    .map((b) => assessBiomarker(b))
    .filter((a) => a.status === "high" || a.status === "low");
}
