import { ImagingStudy, ReportDetail } from "./api";
import { buildHealthSummary } from "./biomarkers";

export type PersonHealthContext = {
  latestLab: ReportDetail | null;
  activeMedicationCount: number;
  latestImaging: ImagingStudy | null;
};

function truncate(text: string, max = 120): string {
  if (text.length <= max) return text;
  return `${text.slice(0, max).trim()}…`;
}

export function buildPersonHealthSummary(context: PersonHealthContext): string {
  const parts: string[] = [];

  if (context.latestLab && context.latestLab.biomarkers.length > 0) {
    parts.push(buildHealthSummary(context.latestLab.biomarkers));
  }

  if (context.activeMedicationCount > 0) {
    const label = context.activeMedicationCount === 1 ? "medication" : "medications";
    parts.push(`You have ${context.activeMedicationCount} active ${label} on file for trend correlation.`);
  }

  if (context.latestImaging?.impression) {
    const modality = context.latestImaging.modality.replace(/_/g, " ");
    const region = context.latestImaging.bodyRegion ? ` (${context.latestImaging.bodyRegion})` : "";
    parts.push(
      `Latest ${modality}${region} from ${context.latestImaging.studyDate}: ${truncate(context.latestImaging.impression)}`
    );
  }

  if (parts.length === 0) {
    return "Upload a lab report, prescription, or scan from Home to build your health timeline.";
  }

  return parts.join(" ");
}
