export type FeatureStatus = "done" | "partial" | "planned";

export type FeatureItem = {
  name: string;
  status: FeatureStatus;
  note: string;
};

export const IMPLEMENTED_FEATURES: FeatureItem[] = [
  { name: "Email + password sign up / sign in", status: "done", note: "Works today" },
  { name: "Fingerprint / face unlock", status: "done", note: "After first login" },
  { name: "Google sign in", status: "partial", note: "Needs full app build (not Expo Go)" },
  { name: "Upload lab PDF or image", status: "done", note: "Local text → rules → optional Claude" },
  { name: "Upload prescription", status: "done", note: "Extract meds → editable confirm → medication log" },
  { name: "Upload imaging / scan report", status: "done", note: "USG, X-ray, CT, MRI → imaging timeline" },
  { name: "Report history + detail view", status: "done", note: "Type-aware detail for lab, Rx, imaging" },
  { name: "Manual parse when auto-parse off", status: "done", note: "Report detail → Parse biomarkers" },
  { name: "Delete wrong report", status: "done", note: "From Reports list" },
  { name: "Family activity feed", status: "done", note: "Home screen with friendly status labels" },
  { name: "Basic vs Advanced mode toggle", status: "done", note: "Settings → Switch mode" },
  { name: "Family switcher", status: "done", note: "Settings → pick active family" },
  { name: "Family roster (advanced)", status: "done", note: "Sidebar → Family" },
  { name: "Upload for family member (advanced)", status: "done", note: "Person picker on upload" },
  { name: "Add family member + invite", status: "done", note: "Family screen → Add / Invite / Share" },
  { name: "Dependent profiles without phone", status: "done", note: "Add profile → Manage health → upload & view as dependent" },
  { name: "Join with invite code", status: "done", note: "Register screen with code preview" },
  { name: "Plain-language health summary", status: "done", note: "Labs + meds + imaging context on Home" },
  { name: "SQL insight cards", status: "done", note: "Home → Insights (rules, changes, trends)" },
  { name: "Medication log", status: "done", note: "Settings — add, end, delete; prescription ingest" },
  { name: "Imaging timeline + study detail", status: "done", note: "Trends → tap study; link to source report" },
  { name: "Out-of-range biomarker highlighting", status: "done", note: "Home + report detail" },
  { name: "YAML clinical rules", status: "done", note: "Deterministic findings at ingest + insights" },
  { name: "Cross-modal imaging + lab rules", status: "done", note: "rules/cross_modal/*.yaml" },
  { name: "Biomarker trends (DuckDB)", status: "done", note: "Sidebar → Trends (advanced)" },
  { name: "Web dashboard", status: "done", note: "Advanced → Dashboard — family snapshot, trends, reminders" },
  { name: "Family biomarker compare", status: "done", note: "Trends → Family compare" },
  { name: "Extraction retry / manual parse", status: "done", note: "POST /reports/{id}/retry and /parse" },
  { name: "Re-test reminders", status: "done", note: "Home → Follow-up tests from biomarker intervals (YAML + SQL)" },
  { name: "Ask (SQL-grounded chat)", status: "done", note: "Advanced → Ask — routes to agent tools, no PDF dumps" },
  { name: "Agent tools API", status: "done", note: "Backend SQL tools used by Ask screen" },
  { name: "Ops pipeline trace", status: "done", note: "Platform admin on report detail" },
];

export const PLANNED_FEATURES: FeatureItem[] = [
  { name: "Hindi / regional UI", status: "planned", note: "Copy localization" },
];

export function statusLabel(status: FeatureStatus): string {
  switch (status) {
    case "done":
      return "Available";
    case "partial":
      return "Partial";
    default:
      return "Coming soon";
  }
}

export function statusColor(status: FeatureStatus): string {
  switch (status) {
    case "done":
      return "#8BA8C4";
    case "partial":
      return "#A8BFD4";
    default:
      return "#6E8198";
  }
}
