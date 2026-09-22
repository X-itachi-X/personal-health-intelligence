export type AgentIntent =
  | { type: "help" }
  | { type: "changes" }
  | { type: "medications" }
  | { type: "medication_context" }
  | { type: "imaging_studies" }
  | { type: "imaging_context" }
  | { type: "abnormal" }
  | { type: "available_biomarkers" }
  | { type: "trend"; canonical: string }
  | { type: "unknown" };

export const SUGGESTED_QUESTIONS = [
  "What changed since my last lab report?",
  "Show my active medications",
  "What's out of range on my latest report?",
  "Is my HbA1c improving?",
  "What imaging scans do I have?",
  "What biomarkers can you look up?",
] as const;

const TREND_HINTS = ["trend", "improving", "worsening", "trajectory", "over time", "history of"];

export function resolveIntent(query: string, availableCanonicals: string[]): AgentIntent {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return { type: "unknown" };
  }

  if (matchesAny(normalized, ["help", "what can you", "what can i ask", "commands"])) {
    return { type: "help" };
  }

  if (matchesAny(normalized, ["what changed", "changes since", "since my last", "delta", "difference"])) {
    return { type: "changes" };
  }

  if (matchesAny(normalized, ["medication context", "meds and lab", "medication correlation"])) {
    return { type: "medication_context" };
  }

  if (matchesAny(normalized, ["medication", "medicine", "prescription", "meds", "taking"])) {
    return { type: "medications" };
  }

  if (matchesAny(normalized, ["imaging context", "scan and lab", "imaging correlation"])) {
    return { type: "imaging_context" };
  }

  if (matchesAny(normalized, ["imaging", "scan", "ultrasound", "usg", "x-ray", "xray", "mri", "ct"])) {
    return { type: "imaging_studies" };
  }

  if (
    matchesAny(normalized, [
      "out of range",
      "abnormal",
      "flagged",
      "attention",
      "concerning",
      "clinical rule",
    ])
  ) {
    return { type: "abnormal" };
  }

  if (matchesAny(normalized, ["what biomarkers", "available biomarkers", "what can you look up", "which tests"])) {
    return { type: "available_biomarkers" };
  }

  const wantsTrend = TREND_HINTS.some((hint) => normalized.includes(hint));
  const canonical = matchCanonical(normalized, availableCanonicals);
  if (canonical && (wantsTrend || normalized.includes(canonical.replace(/_/g, " ")))) {
    return { type: "trend", canonical };
  }

  if (canonical) {
    return { type: "trend", canonical };
  }

  return { type: "unknown" };
}

function matchCanonical(text: string, canonicals: string[]): string | null {
  for (const canonical of canonicals) {
    const spaced = canonical.replace(/_/g, " ");
    if (text.includes(canonical) || text.includes(spaced)) {
      return canonical;
    }
  }

  const aliases: Record<string, string> = {
    hba1c: "hba1c",
    "hb a1c": "hba1c",
    "fasting sugar": "fasting_glucose",
    "blood sugar": "fasting_glucose",
    glucose: "fasting_glucose",
    cholesterol: "ldl_cholesterol",
    ldl: "ldl_cholesterol",
    hdl: "hdl_cholesterol",
    "vitamin d": "vitamin_d",
    tsh: "tsh",
    hemoglobin: "hemoglobin",
    hb: "hemoglobin",
  };

  for (const [alias, canonical] of Object.entries(aliases)) {
    if (text.includes(alias) && (canonicals.includes(canonical) || canonicals.length === 0)) {
      return canonical;
    }
  }

  return null;
}

function matchesAny(text: string, phrases: string[]): boolean {
  return phrases.some((phrase) => text.includes(phrase));
}
