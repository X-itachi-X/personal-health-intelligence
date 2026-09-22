import { Platform } from "react-native";
import { clearSession, getToken } from "./auth";

const API_BASE = process.env.EXPO_PUBLIC_API_URL ?? "http://localhost:8080";

type UnauthorizedListener = () => void;
let unauthorizedListener: UnauthorizedListener | null = null;

/** Called when the server rejects the stored JWT (mid-session expiry, etc.). */
export function setUnauthorizedListener(listener: UnauthorizedListener | null): void {
  unauthorizedListener = listener;
}

export class ApiAuthError extends Error {
  constructor(message = "Not authenticated") {
    super(message);
    this.name = "ApiAuthError";
  }
}

type UploadFile = {
  uri: string;
  name: string;
  mimeType?: string;
  file?: File;
};

async function authHeaders(): Promise<Record<string, string>> {
  const token = await getToken();
  if (!token) {
    return {};
  }
  return { Authorization: `Bearer ${token}` };
}

async function publicFetch(path: string, init: RequestInit = {}): Promise<Response> {
  return fetch(`${API_BASE}${path}`, init);
}

function readApiError(text: string, fallback: string): string {
  if (!text) {
    return fallback;
  }
  try {
    const body = JSON.parse(text) as { message?: string; error?: string };
    if (body.message) {
      return body.message;
    }
    if (body.error) {
      return body.error;
    }
  } catch {
    // plain-text or empty body
  }
  return text || fallback;
}

async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const headers = {
    ...(init.headers as Record<string, string> | undefined),
    ...(await authHeaders()),
  };
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });
  // Only clear session when a token was sent but rejected — not for generic server errors
  // misread as auth failures, and not when the user was already logged out.
  if (response.status === 401 && headers.Authorization) {
    await clearSession();
    unauthorizedListener?.();
  }
  return response;
}

export type DocumentType = "LAB_REPORT" | "PRESCRIPTION" | "IMAGING_REPORT";

type UploadOptions = {
  personId?: number;
  familyId?: string;
  reportDate?: string;
  documentType?: DocumentType;
};

function buildQuery(params: Record<string, string | number | undefined | null>): string {
  const parts = Object.entries(params)
    .filter(([, value]) => value != null && value !== "")
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`);
  return parts.length ? `?${parts.join("&")}` : "";
}

export class DuplicateReportError extends Error {
  existingReportId: number;
  existingFilename: string;
  existingExtractionStatus: string;

  constructor(
    message: string,
    existingReportId: number,
    existingFilename: string,
    existingExtractionStatus = "UNKNOWN"
  ) {
    super(message);
    this.name = "DuplicateReportError";
    this.existingReportId = existingReportId;
    this.existingFilename = existingFilename;
    this.existingExtractionStatus = existingExtractionStatus;
  }
}

function parseUploadError(status: number, text: string): Error {
  try {
    const body = JSON.parse(text) as {
      error?: string;
      message?: string;
      existingReportId?: number;
      existingFilename?: string;
      existingExtractionStatus?: string;
    };
    if (body.error === "duplicate_report" && body.existingReportId != null) {
      return new DuplicateReportError(
        body.message ?? "This file was already uploaded.",
        body.existingReportId,
        body.existingFilename ?? "",
        body.existingExtractionStatus ?? "UNKNOWN"
      );
    }
    if (body.message) {
      return new Error(body.message);
    }
  } catch {
    // not JSON
  }
  return new Error(text || `Upload failed (${status})`);
}

function uploadViaXhr(
  file: UploadFile,
  headers: Record<string, string>,
  options?: UploadOptions
): Promise<UploadResponse> {
  return new Promise((resolve, reject) => {
    const formData = new FormData();
    formData.append("file", {
      uri: file.uri,
      name: file.name,
      type: file.mimeType ?? "application/pdf",
    } as unknown as Blob);

    const xhr = new XMLHttpRequest();
    const query = buildQuery({
      personId: options?.personId,
      familyId: options?.familyId,
      reportDate: options?.reportDate,
      documentType: options?.documentType,
    });
    xhr.open("POST", `${API_BASE}/api/v1/reports${query}`);
    Object.entries(headers).forEach(([key, value]) => xhr.setRequestHeader(key, value));
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          resolve(JSON.parse(xhr.responseText) as UploadResponse);
        } catch {
          reject(new Error("Invalid upload response from server"));
        }
        return;
      }
      reject(parseUploadError(xhr.status, xhr.responseText));
    };
    xhr.onerror = () => reject(new Error("Network error during upload"));
    xhr.send(formData);
  });
}

async function uploadViaFetch(
  file: UploadFile,
  headers: Record<string, string>,
  options?: UploadOptions
): Promise<UploadResponse> {
  let body: File | Blob;
  if (file.file) {
    body = file.file;
  } else {
    const response = await fetch(file.uri);
    if (!response.ok) {
      throw new Error(`Could not read selected file (${response.status})`);
    }
    const blob = await response.blob();
    body = new File([blob], file.name, {
      type: file.mimeType ?? "application/pdf",
    });
  }

  const formData = new FormData();
  formData.append("file", body, file.name);

  const query = buildQuery({
    personId: options?.personId,
    familyId: options?.familyId,
    reportDate: options?.reportDate,
    documentType: options?.documentType,
  });
  const response = await fetch(`${API_BASE}/api/v1/reports${query}`, {
    method: "POST",
    headers,
    body: formData,
  });

  if (!response.ok) {
    const text = await response.text();
    throw parseUploadError(response.status, text);
  }

  return response.json() as Promise<UploadResponse>;
}

export type HealthResponse = {
  status: string;
  service: string;
};

export type AuthResponse = {
  token: string;
  accountId: string;
  personId: number;
  email: string;
  displayName: string;
  uiMode: "basic" | "advanced";
  platformAdmin?: boolean;
};

export type RegisterRequest = {
  email: string;
  password: string;
  displayName: string;
  dateOfBirth: string;
  sex: string;
  relationship: string;
  inviteCode?: string;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type UploadResponse = {
  reportId: number;
  filename: string;
  uploadedAt: string;
  extractionStatus: string;
  message: string;
};

export type Biomarker = {
  canonical: string;
  testName: string;
  value: number | null;
  textValue: string | null;
  unit: string | null;
  referenceRange: string | null;
  confidence: number | null;
};

export type ReportSummary = {
  reportId: number;
  filename: string;
  uploadedAt: string;
  extractionStatus: string;
  documentType?: DocumentType;
  personId: number;
  personName: string;
};

export type ReportDetail = {
  reportId: number;
  filename: string;
  uploadedAt: string;
  reportDate?: string | null;
  reportDateSource?: "EXTRACTED" | "USER" | null;
  needsReportDate?: boolean;
  canCorrectReportDate?: boolean;
  extractionStatus: string;
  extractionError: string | null;
  personId?: number;
  familyId?: string;
  biomarkerCount: number;
  biomarkers: Biomarker[];
  extractedTextLength?: number;
  hasStoredText?: boolean;
  hasReadableFile?: boolean;
  canRetry?: boolean;
  documentType?: DocumentType;
  needsMedicationConfirmation?: boolean;
  needsImagingConfirmation?: boolean;
  prescriptionItems?: ExtractedPrescriptionItem[];
  imagingStudy?: ExtractedImagingStudy;
};

export type ExtractedImagingFinding = {
  findingText: string;
  severity: string | null;
  measurementValue: string | null;
  measurementUnit: string | null;
};

export type ExtractedImagingStudy = {
  modality: string;
  bodyRegion: string | null;
  studyDate: string | null;
  facility: string | null;
  impression: string | null;
  findings: ExtractedImagingFinding[];
};

export type ImagingStudy = {
  id: string;
  personId: number;
  modality: string;
  bodyRegion: string | null;
  studyDate: string;
  facility: string | null;
  impression: string | null;
  sourceReportId: number | null;
  findings: {
    id: string;
    findingText: string;
    severity: string | null;
    measurementValue: string | null;
    measurementUnit: string | null;
  }[];
};

export type ExtractedPrescriptionItem = {
  medicationName: string;
  dosage: string | null;
  scheduleText: string | null;
  startedOn: string | null;
  durationDays: number | null;
  courseType: MedicationCourseType;
};

export type FeedItem = {
  reportId: number;
  personName: string;
  filename: string;
  extractionStatus: string;
  uploadedAt: string;
};

export type FamilySummary = {
  id: string;
  displayName: string;
  myRole: string;
  myRelationship: string;
  memberCount: number;
};

export type FamilyMember = {
  personId: number;
  displayName: string;
  relationship: string;
  role: string;
  hasAccount: boolean;
};

export type FamilyDetail = {
  id: string;
  displayName: string;
  members: FamilyMember[];
};

export type AddMemberRequest = {
  displayName: string;
  dateOfBirth: string;
  sex: string;
  relationship: string;
  role: string;
};

export type InviteResponse = {
  id: string;
  code: string;
  familyId: string;
  personId: number;
  expiresAt: string;
};

export type InvitePreview = {
  code: string;
  familyName: string | null;
  personName: string | null;
  valid: boolean;
};

export type OpsSummary = {
  days: number;
  totalEvents: number;
  totalInputTokens: number;
  totalOutputTokens: number;
  aiParseCalls: number;
  aiVisionCalls: number;
  ruleSuccesses: number;
  failures: number;
  eventsByType: Record<string, number>;
};

export type OpsDayBucket = {
  date: string;
  totalTokens: number;
  failures: number;
  uploads: number;
};

export type OpsTimeseries = {
  days: number;
  buckets: OpsDayBucket[];
};

export type ExtractionEventView = {
  id: string;
  reportId: number;
  familyId: string | null;
  eventType: string;
  status: string;
  durationMs: number | null;
  charCount: number | null;
  biomarkerCount: number | null;
  coverage: number | null;
  inputTokens: number | null;
  outputTokens: number | null;
  model: string | null;
  message: string | null;
  textPreview: string | null;
  createdAt: string;
};

export type ReportPipelineView = {
  reportId: number;
  filename: string;
  extractionStatus: string;
  extractionError: string | null;
  uploadedAt: string;
  textExtractionTool: string;
  biomarkerParseTool: string;
  extractedText: string;
  extractedTextLength: number;
  originalFileAvailable: boolean;
  originalFileSizeBytes: number | null;
  originalContentType: string | null;
  events: ExtractionEventView[];
  totalInputTokens: number;
  totalOutputTokens: number;
};

export type BiomarkerPreview = {
  canonical: string | null;
  testName: string;
  value: string | null;
  unit: string | null;
};

export type FreeToolResult = {
  toolId: string;
  label: string;
  category: "text" | "parse";
  sourceToolId: string | null;
  status: string;
  durationMs: number | null;
  charCount: number | null;
  biomarkerCount: number | null;
  coverage: number | null;
  labFormat: string | null;
  text: string | null;
  error: string | null;
  biomarkers: BiomarkerPreview[];
};

export type FreeToolComparisonView = {
  reportId: number;
  filename: string;
  originalFileAvailable: boolean;
  originalFileSizeBytes: number | null;
  originalContentType: string | null;
  fileUnavailableReason: string | null;
  tools: FreeToolResult[];
};

export type AuditEventView = {
  id: string;
  action: string;
  actorAccountId: string | null;
  targetType: string | null;
  targetId: string | null;
  metadata: string | null;
  createdAt: string;
};

export async function fetchOpsSummary(days = 7): Promise<OpsSummary> {
  const response = await apiFetch(`/api/v1/ops/summary?days=${days}`);
  if (response.status === 403) {
    throw new Error("Platform operator access required");
  }
  if (!response.ok) {
    throw new Error(`Failed to load ops summary (${response.status})`);
  }
  return response.json();
}

export async function fetchOpsTimeseries(days = 7): Promise<OpsTimeseries> {
  const response = await apiFetch(`/api/v1/ops/timeseries?days=${days}`);
  if (!response.ok) {
    throw new Error(`Failed to load ops timeseries (${response.status})`);
  }
  return response.json();
}

export type ReportOpsRow = {
  reportId: number;
  filename: string;
  extractionStatus: string;
  extractionError: string | null;
  personId: number;
  personName: string;
  familyId: string | null;
  uploadedByAccountId: string | null;
  uploadedAt: string;
};

export async function fetchOpsEvents(limit = 50, familyId?: string): Promise<ExtractionEventView[]> {
  const query = buildQuery({ familyId, limit });
  const response = await apiFetch(`/api/v1/ops/events${query}`);
  if (response.status === 403) {
    throw new Error("Platform operator access required");
  }
  if (!response.ok) {
    throw new Error(`Failed to load ops events (${response.status})`);
  }
  return response.json();
}

export async function fetchOpsReports(limit = 50): Promise<ReportOpsRow[]> {
  const query = buildQuery({ limit });
  const response = await apiFetch(`/api/v1/ops/reports${query}`);
  if (!response.ok) {
    throw new Error(`Failed to load ops reports (${response.status})`);
  }
  return response.json();
}

export async function fetchOpsErrors(days = 7, limit = 30): Promise<ExtractionEventView[]> {
  const query = buildQuery({ days, limit });
  const response = await apiFetch(`/api/v1/ops/errors${query}`);
  if (!response.ok) {
    throw new Error(`Failed to load ops errors (${response.status})`);
  }
  return response.json();
}

export async function fetchReportPipeline(reportId: number): Promise<ReportPipelineView> {
  const response = await apiFetch(`/api/v1/ops/reports/${reportId}/pipeline`);
  if (!response.ok) {
    throw new Error(`Failed to load pipeline (${response.status})`);
  }
  return response.json();
}

export async function fetchFreeToolComparison(
  reportId: number,
  refresh = false
): Promise<FreeToolComparisonView> {
  const query = buildQuery({ refresh: refresh ? "true" : undefined });
  const response = await apiFetch(`/api/v1/ops/reports/${reportId}/free-tools${query}`);
  if (!response.ok) {
    throw new Error(`Failed to compare free tools (${response.status})`);
  }
  return response.json();
}

export async function downloadOpsReportFile(reportId: number, filename: string): Promise<void> {
  const response = await apiFetch(`/api/v1/ops/reports/${reportId}/file`);
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Download failed (${response.status})`);
  }
  const blob = await response.blob();
  if (Platform.OS === "web" && typeof document !== "undefined") {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
    return;
  }
  throw new Error("File download is supported on web ops console. Use free-tool comparison on mobile.");
}

export async function fetchOpsAudit(limit = 50, familyId?: string): Promise<AuditEventView[]> {
  const query = buildQuery({ familyId, limit });
  const response = await apiFetch(`/api/v1/ops/audit${query}`);
  if (!response.ok) {
    throw new Error(`Failed to load audit log (${response.status})`);
  }
  return response.json();
}

export type FeedbackCategory = "praise" | "bug" | "improvement" | "other";
export type FeedbackStatus = "pending" | "read" | "resolved";

export type SubmitFeedbackRequest = {
  category: FeedbackCategory;
  message: string;
  rating?: number;
  screenContext?: string;
  appPlatform?: string;
  appVersion?: string;
};

export type FeedbackView = {
  id: string;
  accountId: string;
  submitterEmail: string;
  submitterName: string;
  category: FeedbackCategory;
  message: string;
  rating: number | null;
  screenContext: string | null;
  appPlatform: string | null;
  appVersion: string | null;
  status: FeedbackStatus;
  createdAt: string;
};

export async function submitFeedback(request: SubmitFeedbackRequest): Promise<{ id: string; createdAt: string }> {
  const response = await apiFetch("/api/v1/feedback", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(readApiError(text, `Failed to send feedback (${response.status})`));
  }
  return response.json();
}

export async function fetchOpsFeedback(): Promise<FeedbackView[]> {
  const response = await apiFetch("/api/v1/ops/feedback");
  if (!response.ok) {
    throw new Error(`Failed to load tester feedback (${response.status})`);
  }
  return response.json();
}

export async function updateFeedbackStatus(
  feedbackId: string,
  status: FeedbackStatus
): Promise<FeedbackView> {
  const response = await apiFetch(`/api/v1/ops/feedback/${feedbackId}`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status }),
  });
  if (!response.ok) {
    throw new Error(`Failed to update feedback (${response.status})`);
  }
  return response.json();
}

export async function fetchHealth(): Promise<HealthResponse> {
  const response = await fetch(`${API_BASE}/api/v1/health`);
  if (!response.ok) {
    throw new Error(`Health check failed (${response.status})`);
  }
  return response.json();
}

export async function register(request: RegisterRequest): Promise<AuthResponse> {
  const response = await publicFetch("/api/v1/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(readApiError(text, `Registration failed (${response.status})`));
  }
  return response.json();
}

export async function loginWithGoogle(idToken: string): Promise<AuthResponse> {
  const response = await publicFetch("/api/v1/auth/google", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ idToken }),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(readApiError(text, `Google sign-in failed (${response.status})`));
  }
  return response.json();
}

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const response = await publicFetch("/api/v1/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(readApiError(text, `Login failed (${response.status})`));
  }
  return response.json();
}

export async function updateUiMode(uiMode: "basic" | "advanced"): Promise<AuthResponse> {
  const response = await apiFetch("/api/v1/auth/me/ui-mode", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ uiMode }),
  });
  if (!response.ok) {
    throw new Error(`Failed to update mode (${response.status})`);
  }
  const me = await response.json();
  return {
    token: (await getToken()) ?? "",
    accountId: me.accountId,
    personId: me.personId,
    email: me.email,
    displayName: me.displayName,
    uiMode: me.uiMode,
    platformAdmin: me.platformAdmin ?? false,
  };
}

export async function listReports(familyId?: string): Promise<ReportSummary[]> {
  const query = buildQuery({ familyId });
  const response = await apiFetch(`/api/v1/reports${query}`);
  if (!response.ok) {
    throw new Error(`Failed to list reports (${response.status})`);
  }
  return response.json();
}

export async function deleteReport(reportId: number): Promise<void> {
  const response = await apiFetch(`/api/v1/reports/${reportId}`, { method: "DELETE" });
  if (!response.ok) {
    throw new Error(`Failed to delete report (${response.status})`);
  }
}

export type RetryReportResponse = {
  reportId: number;
  extractionStatus: string;
  message: string;
};

export async function retryReport(reportId: number): Promise<RetryReportResponse> {
  const response = await apiFetch(`/api/v1/reports/${reportId}/retry`, { method: "POST" });
  if (response.status === 409) {
    const text = await response.text();
    throw new Error(text || "Extraction already in progress");
  }
  if (!response.ok) {
    throw new Error(`Failed to retry extraction (${response.status})`);
  }
  return response.json();
}

export async function parseReport(reportId: number): Promise<RetryReportResponse> {
  const response = await apiFetch(`/api/v1/reports/${reportId}/parse`, { method: "POST" });
  if (response.status === 409) {
    const text = await response.text();
    throw new Error(text || "Parsing already in progress");
  }
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed to start parsing (${response.status})`);
  }
  return response.json();
}

export async function submitReportDate(
  reportId: number,
  reportDate: string,
  options?: { correct?: boolean }
): Promise<{ reportId: number; reportDate: string; extractionStatus: string; message: string }> {
  const response = await apiFetch(`/api/v1/reports/${reportId}/report-date`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ reportDate, correct: options?.correct ?? false }),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed to save report date (${response.status})`);
  }
  return response.json();
}

export async function replaceFailedReport(
  reportId: number,
  file: UploadFile,
  reportDate?: string
): Promise<UploadResponse> {
  const headers = await authHeaders();
  const formData = new FormData();
  if (Platform.OS === "web" && file.file) {
    formData.append("file", file.file);
  } else {
    formData.append("file", {
      uri: file.uri,
      name: file.name,
      type: file.mimeType ?? "application/pdf",
    } as unknown as Blob);
  }

  const query = buildQuery({ reportDate });
  const response = await fetch(`${API_BASE}/api/v1/reports/${reportId}/replace${query}`, {
    method: "POST",
    headers,
    body: formData,
  });
  if (!response.ok) {
    const text = await response.text();
    throw parseUploadError(response.status, text);
  }
  return response.json();
}

export async function fetchFamilyFeed(familyId?: string): Promise<FeedItem[]> {
  const query = buildQuery({ familyId });
  const response = await apiFetch(`/api/v1/family/feed${query}`);
  if (!response.ok) {
    throw new Error(`Failed to load feed (${response.status})`);
  }
  return response.json();
}

export async function listFamilies(): Promise<FamilySummary[]> {
  const response = await apiFetch("/api/v1/family");
  if (response.status === 401 || response.status === 403) {
    return [];
  }
  if (!response.ok) {
    throw new Error(`Failed to list families (${response.status})`);
  }
  return response.json();
}

export async function getFamily(familyId: string): Promise<FamilyDetail> {
  const response = await apiFetch(`/api/v1/family/${familyId}`);
  if (!response.ok) {
    throw new Error(`Failed to load family (${response.status})`);
  }
  return response.json();
}

export async function addFamilyMember(
  familyId: string,
  request: AddMemberRequest
): Promise<FamilyMember> {
  const response = await apiFetch(`/api/v1/family/${familyId}/members`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed to add member (${response.status})`);
  }
  return response.json();
}

export async function createInvite(familyId: string, personId: number): Promise<InviteResponse> {
  const response = await apiFetch("/api/v1/invites", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ familyId, personId }),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed to create invite (${response.status})`);
  }
  return response.json();
}

export async function previewInvite(code: string): Promise<InvitePreview> {
  const response = await fetch(`${API_BASE}/api/v1/invites/${encodeURIComponent(code)}`);
  if (!response.ok) {
    throw new Error(`Invalid invite (${response.status})`);
  }
  return response.json();
}

export type SeedDemoResponse = {
  alreadySeeded: boolean;
  families: number;
  membersAdded: number;
  reportsAdded: number;
  scenariosLoaded: number;
  message: string;
};

export type TrendPoint = {
  date: string;
  value: number;
  unit: string | null;
  reportId: number;
};

export type TrendInsight = {
  direction:
    | "INSUFFICIENT_DATA"
    | "STABLE"
    | "INCREASING"
    | "DECREASING"
    | "FLUCTUATING";
  summary: string;
  pointCount: number;
  latestValue: number | null;
  previousValue: number | null;
  delta: number | null;
  percentChange: number | null;
};

export type PersonTrendResponse = {
  personId: number;
  canonical: string;
  unit: string | null;
  points: TrendPoint[];
  insight: TrendInsight;
};

export type ReportFinding = {
  kind: "REFERENCE_RANGE" | "CLINICAL_RULE";
  ruleId: string | null;
  canonical: string | null;
  testName: string | null;
  value: number | null;
  unit: string | null;
  referenceRange: string | null;
  severity: string | null;
  action: string | null;
  message: string;
};

export type FamilyHealthSnapshot = {
  familyId: string;
  memberCount: number;
  reportsWithData: number;
  outOfRangeMembers: number;
  members: {
    personId: number;
    personName: string;
    outOfRangeCount: number;
    latestReportDate: string | null;
  }[];
};

export type ReportFindingsResponse = {
  reportId: number;
  reportDate: string | null;
  biomarkerCount: number;
  outOfRangeCount: number;
  findings: ReportFinding[];
};

export type FamilyCompareMember = {
  personId: number;
  personName: string;
  points: TrendPoint[];
};

export type FamilyCompareResponse = {
  canonical: string;
  members: FamilyCompareMember[];
};

export async function fetchAvailableBiomarkers(personId: number): Promise<string[]> {
  const response = await apiFetch(`/api/v1/analytics/persons/${personId}/biomarkers`);
  if (!response.ok) {
    throw new Error(`Failed to load biomarkers (${response.status})`);
  }
  return response.json();
}

export type BiomarkerChange = {
  canonical: string;
  testName: string | null;
  unit: string | null;
  latestValue: number | null;
  previousValue: number | null;
  delta: number | null;
  percentChange: number | null;
  direction: "UP" | "DOWN" | "UNCHANGED" | "NEW" | "REMOVED" | null;
  referenceRange: string | null;
};

export type PersonChangesResponse = {
  personId: number;
  latestReportId: number | null;
  latestReportDate: string | null;
  previousReportId: number | null;
  previousReportDate: string | null;
  comparedBiomarkers: number;
  changes: BiomarkerChange[];
};

export type InsightCard = {
  id: string;
  type: string;
  title: string;
  message: string;
  severity: "info" | "warning" | "success";
  canonical: string | null;
  reportId: number | null;
};

export type PersonInsightsResponse = {
  personId: number;
  cards: InsightCard[];
};

export type MedicationCourseType = "ACUTE" | "CHRONIC" | "UNKNOWN";

export type Medication = {
  id: string;
  personId: number;
  medicationName: string;
  dosage: string | null;
  startedOn: string | null;
  endedOn: string | null;
  notes: string | null;
  courseType: MedicationCourseType;
  scheduleText: string | null;
  durationDays: number | null;
  expectedEndOn: string | null;
  source: "MANUAL" | "PRESCRIPTION";
  sourceReportId: number | null;
  active: boolean;
};

export async function fetchPersonChanges(personId: number): Promise<PersonChangesResponse> {
  const response = await apiFetch(`/api/v1/analytics/persons/${personId}/changes`);
  if (!response.ok) {
    throw new Error(`Failed to load changes (${response.status})`);
  }
  return response.json();
}

export async function fetchPersonInsights(personId: number): Promise<PersonInsightsResponse> {
  const response = await apiFetch(`/api/v1/analytics/persons/${personId}/insights`);
  if (!response.ok) {
    throw new Error(`Failed to load insights (${response.status})`);
  }
  return response.json();
}

export type RetestReminder = {
  id: string;
  label: string;
  canonical: string | null;
  lastTestDate: string;
  dueDate: string;
  urgency: "OVERDUE" | "DUE_SOON" | "UPCOMING";
  reason: "ABNORMAL_LAST" | "ROUTINE";
  message: string;
  reportId: number | null;
};

export type PersonRemindersResponse = {
  personId: number;
  summary: string;
  reminders: RetestReminder[];
};

export async function fetchPersonReminders(personId: number): Promise<PersonRemindersResponse> {
  const response = await apiFetch(`/api/v1/analytics/persons/${personId}/reminders`);
  if (!response.ok) {
    throw new Error(`Failed to load reminders (${response.status})`);
  }
  return response.json();
}

export async function listMedications(personId: number): Promise<Medication[]> {
  const response = await apiFetch(`/api/v1/persons/${personId}/medications`);
  if (!response.ok) {
    throw new Error(`Failed to load medications (${response.status})`);
  }
  return response.json();
}

export async function createMedication(
  personId: number,
  body: {
    medicationName: string;
    dosage?: string;
    startedOn: string;
    notes?: string;
    courseType?: MedicationCourseType;
    scheduleText?: string;
    durationDays?: number;
    expectedEndOn?: string;
  }
): Promise<Medication> {
  const response = await apiFetch(`/api/v1/persons/${personId}/medications`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed to add medication (${response.status})`);
  }
  return response.json();
}

export async function endMedication(
  personId: number,
  medicationId: string,
  endedOn?: string
): Promise<Medication> {
  const response = await apiFetch(`/api/v1/persons/${personId}/medications/${medicationId}/end`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(endedOn ? { endedOn } : {}),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed to end medication (${response.status})`);
  }
  return response.json();
}

export async function deleteMedication(personId: number, medicationId: string): Promise<void> {
  const response = await apiFetch(`/api/v1/persons/${personId}/medications/${medicationId}`, {
    method: "DELETE",
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed to delete medication (${response.status})`);
  }
}

export async function fetchPersonTrend(personId: number, canonical: string): Promise<PersonTrendResponse> {
  const query = `?canonical=${encodeURIComponent(canonical)}`;
  const response = await apiFetch(`/api/v1/analytics/persons/${personId}/trends${query}`);
  if (!response.ok) {
    throw new Error(`Failed to load trend (${response.status})`);
  }
  return response.json();
}

export async function fetchFamilyCompare(familyId: string, canonical: string): Promise<FamilyCompareResponse> {
  const query = `?canonical=${encodeURIComponent(canonical)}`;
  const response = await apiFetch(`/api/v1/analytics/family/${familyId}/compare${query}`);
  if (!response.ok) {
    throw new Error(`Failed to load family compare (${response.status})`);
  }
  return response.json();
}

export async function rebuildAnalytics(): Promise<{ personsSynced: number; biomarkersSynced: number; message: string }> {
  const response = await apiFetch("/api/v1/dev/analytics-rebuild", { method: "POST" });
  if (!response.ok) {
    throw new Error(`Failed to rebuild analytics (${response.status})`);
  }
  return response.json();
}

export async function seedDemoData(): Promise<SeedDemoResponse> {
  const response = await apiFetch("/api/v1/dev/seed", { method: "POST" });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed to seed demo data (${response.status})`);
  }
  return response.json();
}

export async function fetchMe(): Promise<AuthResponse> {
  const response = await apiFetch("/api/v1/auth/me");
  if (response.status === 401) {
    throw new ApiAuthError();
  }
  if (!response.ok) {
    throw new Error(`Failed to load profile (${response.status})`);
  }
  const me = await response.json();
  return {
    token: (await getToken()) ?? "",
    accountId: me.accountId,
    personId: me.personId,
    email: me.email,
    displayName: me.displayName,
    uiMode: me.uiMode,
    platformAdmin: me.platformAdmin ?? false,
  };
}

export async function fetchReportFindings(reportId: number): Promise<ReportFindingsResponse> {
  const response = await apiFetch(`/api/v1/reports/${reportId}/findings`);
  if (!response.ok) {
    throw new Error(`Failed to fetch findings (${response.status})`);
  }
  return response.json();
}

export async function fetchFamilySnapshot(familyId: string): Promise<FamilyHealthSnapshot> {
  const response = await apiFetch(`/api/v1/analytics/family/${familyId}/snapshot`);
  if (!response.ok) {
    throw new Error(`Failed to load family snapshot (${response.status})`);
  }
  return response.json();
}

export async function listImagingStudies(personId: number): Promise<ImagingStudy[]> {
  const response = await apiFetch(`/api/v1/persons/${personId}/imaging-studies`);
  if (!response.ok) {
    throw new Error(`Failed to load imaging studies (${response.status})`);
  }
  return response.json();
}

export async function fetchImagingStudy(personId: number, studyId: string): Promise<ImagingStudy> {
  const response = await apiFetch(`/api/v1/persons/${personId}/imaging-studies/${studyId}`);
  if (!response.ok) {
    throw new Error(`Failed to load imaging study (${response.status})`);
  }
  return response.json();
}

export async function confirmImagingStudy(
  reportId: number,
  study: {
    modality: string;
    bodyRegion?: string;
    studyDate: string;
    facility?: string;
    impression: string;
    findings?: ExtractedImagingFinding[];
  }
): Promise<{ imagingStudyId: string }> {
  const response = await apiFetch(`/api/v1/reports/${reportId}/confirm-imaging`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(study),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed to confirm imaging study (${response.status})`);
  }
  return response.json();
}

export async function confirmPrescriptionMedications(
  reportId: number,
  medications: {
    medicationName: string;
    dosage?: string;
    scheduleText?: string;
    startedOn: string;
    durationDays?: number;
    courseType?: MedicationCourseType;
  }[]
): Promise<{ savedCount: number; medicationIds: string[] }> {
  const response = await apiFetch(`/api/v1/reports/${reportId}/confirm-medications`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ medications }),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed to confirm medications (${response.status})`);
  }
  return response.json();
}

export async function fetchReport(reportId: number): Promise<ReportDetail> {
  const response = await apiFetch(`/api/v1/reports/${reportId}`);
  if (!response.ok) {
    throw new Error(`Failed to fetch report (${response.status})`);
  }
  return response.json();
}

export async function pollReportUntilDone(
  reportId: number,
  onUpdate: (report: ReportDetail) => void,
  maxAttempts = 90,
  intervalMs = 2000
): Promise<ReportDetail> {
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const report = await fetchReport(reportId);
    onUpdate(report);

    if (report.extractionStatus === "COMPLETED" || report.extractionStatus === "TEXT_ONLY") {
      return report;
    }
    if (report.extractionStatus === "AWAITING_REPORT_DATE") {
      return report;
    }
    if (report.extractionStatus === "AWAITING_MEDICATION_CONFIRMATION") {
      return report;
    }
    if (report.extractionStatus === "AWAITING_IMAGING_CONFIRMATION") {
      return report;
    }
    if (report.extractionStatus === "FAILED") {
      throw new Error(report.extractionError ?? "Extraction failed");
    }

    await new Promise((resolve) => setTimeout(resolve, intervalMs));
  }

  throw new Error("Extraction timed out");
}

export async function uploadReport(
  file: UploadFile,
  options?: UploadOptions
): Promise<UploadResponse> {
  console.log("[phi] uploading", file.name, "via", Platform.OS, "to", `${API_BASE}/api/v1/reports`);
  const headers = await authHeaders();

  try {
    const result =
      Platform.OS === "web"
        ? await uploadViaFetch(file, headers, options)
        : await uploadViaXhr(file, headers, options);
    console.log("[phi] upload ok", result.reportId, result.extractionStatus);
    return result;
  } catch (error) {
    console.error("[phi] upload failed", error);
    throw error;
  }
}

export async function agentFetchChanges(personId: number): Promise<PersonChangesResponse> {
  const response = await apiFetch(`/api/v1/agent/tools/persons/${personId}/changes`);
  if (!response.ok) {
    throw new Error(`Failed to load changes (${response.status})`);
  }
  return response.json();
}

export async function agentFetchTrend(personId: number, canonical: string): Promise<PersonTrendResponse> {
  const query = `?canonical=${encodeURIComponent(canonical)}`;
  const response = await apiFetch(`/api/v1/agent/tools/persons/${personId}/timeline${query}`);
  if (!response.ok) {
    throw new Error(`Failed to load trend (${response.status})`);
  }
  return response.json();
}

export async function agentFetchAbnormal(reportId: number): Promise<ReportFindingsResponse> {
  const response = await apiFetch(`/api/v1/agent/tools/reports/${reportId}/abnormal`);
  if (!response.ok) {
    throw new Error(`Failed to load findings (${response.status})`);
  }
  return response.json();
}

export async function agentFetchMedications(personId: number): Promise<Medication[]> {
  const response = await apiFetch(`/api/v1/agent/tools/persons/${personId}/medications`);
  if (!response.ok) {
    throw new Error(`Failed to load medications (${response.status})`);
  }
  return response.json();
}

export async function agentFetchMedicationContext(personId: number): Promise<InsightCard[]> {
  const response = await apiFetch(`/api/v1/agent/tools/persons/${personId}/medication-context`);
  if (!response.ok) {
    throw new Error(`Failed to load medication context (${response.status})`);
  }
  return response.json();
}

export async function agentFetchImagingStudies(personId: number): Promise<ImagingStudy[]> {
  const response = await apiFetch(`/api/v1/agent/tools/persons/${personId}/imaging-studies`);
  if (!response.ok) {
    throw new Error(`Failed to load imaging studies (${response.status})`);
  }
  return response.json();
}

export async function agentFetchImagingContext(personId: number): Promise<InsightCard[]> {
  const response = await apiFetch(`/api/v1/agent/tools/persons/${personId}/imaging-context`);
  if (!response.ok) {
    throw new Error(`Failed to load imaging context (${response.status})`);
  }
  return response.json();
}

export async function agentFetchAvailableBiomarkers(personId: number): Promise<string[]> {
  const response = await apiFetch(`/api/v1/agent/tools/persons/${personId}/available-biomarkers`);
  if (!response.ok) {
    throw new Error(`Failed to load biomarkers (${response.status})`);
  }
  return response.json();
}

export { API_BASE };
