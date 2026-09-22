import {
  BiomarkerChange,
  ImagingStudy,
  InsightCard,
  Medication,
  PersonChangesResponse,
  PersonTrendResponse,
  ReportFindingsResponse,
} from "../api";
import { formatBiomarkerName } from "../biomarkers";

export function formatHelpMessage(): string {
  return [
    "I answer from your structured health data in the database — not by re-reading PDFs.",
    "",
    "Try asking about:",
    "• What changed since your last lab report",
    "• Active medications or imaging scans",
    "• Trends for a biomarker (e.g. HbA1c, vitamin D)",
    "• Out-of-range results on your latest report",
    "• Available biomarkers you can query",
  ].join("\n");
}

export function formatUnknownMessage(): string {
  return "I didn't recognize that question. Tap a suggestion below or ask about changes, medications, imaging, trends, or out-of-range results.";
}

export function formatChanges(response: PersonChangesResponse): string {
  if (!response.previousReportDate) {
    return "You need at least two completed lab reports before I can compare what changed.";
  }

  const meaningful = response.changes.filter(
    (change) => change.direction === "UP" || change.direction === "DOWN"
  );

  if (meaningful.length === 0) {
    return `I compared ${response.comparedBiomarkers} biomarkers between ${response.previousReportDate} and ${response.latestReportDate}. No major shifts stood out.`;
  }

  const lines = meaningful.slice(0, 8).map((change) => formatChangeLine(change));
  return [
    `Changes between ${response.previousReportDate} and ${response.latestReportDate}:`,
    "",
    ...lines,
  ].join("\n");
}

function formatChangeLine(change: BiomarkerChange): string {
  const name = change.testName ?? formatBiomarkerName(change.canonical);
  const unit = change.unit ? ` ${change.unit}` : "";
  const arrow = change.direction === "UP" ? "↑" : "↓";
  const pct = change.percentChange != null ? ` (${Math.abs(change.percentChange).toFixed(1)}%)` : "";
  return `• ${name}: ${change.previousValue} → ${change.latestValue}${unit} ${arrow}${pct}`;
}

export function formatMedications(medications: Medication[]): string {
  const active = medications.filter((med) => med.active);
  if (active.length === 0) {
    return "No active medications on file. Upload a prescription from Home to extract them automatically.";
  }

  const lines = active.map((med) => {
    const course =
      med.courseType === "CHRONIC" ? "ongoing" : med.courseType === "ACUTE" ? "short course" : "medication";
    const schedule = med.scheduleText ? ` · ${med.scheduleText}` : "";
    return `• ${med.medicationName} (${course}${schedule})`;
  });

  return ["Active medications:", "", ...lines].join("\n");
}

export function formatInsightCards(title: string, cards: InsightCard[]): string {
  if (cards.length === 0) {
    return `No ${title} insights right now — upload more labs or confirm medications/imaging to improve correlation.`;
  }

  return [title + ":", "", ...cards.map((card) => `• ${card.title}: ${card.message}`)].join("\n");
}

export function formatImagingStudies(studies: ImagingStudy[]): string {
  if (studies.length === 0) {
    return "No imaging studies saved yet. Upload a scan report from Home and confirm the findings.";
  }

  const lines = studies.slice(0, 5).map((study) => {
    const modality = study.modality.replace(/_/g, " ");
    const region = study.bodyRegion ? ` · ${study.bodyRegion}` : "";
    const impression = study.impression ? ` — ${study.impression}` : "";
    return `• ${modality}${region} (${study.studyDate})${impression}`;
  });

  return ["Imaging timeline:", "", ...lines].join("\n");
}

export function formatAbnormal(findings: ReportFindingsResponse): string {
  if (findings.findings.length === 0) {
    return "Your latest report has no flagged clinical rules or out-of-range biomarkers in the database.";
  }

  const lines = findings.findings.slice(0, 10).map((finding) => `• ${finding.message}`);
  return [
    `${findings.outOfRangeCount} flagged result(s) on report from ${findings.reportDate ?? "unknown date"}:`,
    "",
    ...lines,
  ].join("\n");
}

export function formatAvailableBiomarkers(canonicals: string[]): string {
  if (canonicals.length === 0) {
    return "No biomarkers in the database yet. Upload a completed lab report first.";
  }

  const sample = canonicals.slice(0, 12).map((c) => formatBiomarkerName(c)).join(", ");
  const more = canonicals.length > 12 ? ` …and ${canonicals.length - 12} more.` : "";
  return `You can ask about trends for ${canonicals.length} biomarkers, e.g. ${sample}${more}`;
}

export function formatTrend(trend: PersonTrendResponse): string {
  const name = formatBiomarkerName(trend.canonical);
  if (trend.points.length === 0) {
    return `No ${name} data points found yet.`;
  }

  const latest = trend.points[trend.points.length - 1];
  const unit = trend.unit ? ` ${trend.unit}` : "";
  const lines = [
    `${name} trend (${trend.insight.direction.toLowerCase().replace(/_/g, " ")}):`,
    trend.insight.summary,
    "",
    `Latest: ${latest.value}${unit} on ${latest.date}`,
  ];

  if (trend.points.length > 1) {
    const first = trend.points[0];
    lines.push(`First on file: ${first.value}${unit} on ${first.date}`);
  }

  return lines.join("\n");
}
