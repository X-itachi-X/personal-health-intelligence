import { Platform } from "react-native";

const API_BASE = process.env.EXPO_PUBLIC_API_URL ?? "http://localhost:8080";

type UploadFile = {
  uri: string;
  name: string;
  mimeType?: string;
  file?: File;
};

function uploadViaXhr(file: UploadFile): Promise<UploadResponse> {
  return new Promise((resolve, reject) => {
    const formData = new FormData();
    formData.append("file", {
      uri: file.uri,
      name: file.name,
      type: file.mimeType ?? "application/pdf",
    } as unknown as Blob);

    const xhr = new XMLHttpRequest();
    xhr.open("POST", `${API_BASE}/api/v1/reports`);
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          resolve(JSON.parse(xhr.responseText) as UploadResponse);
        } catch {
          reject(new Error("Invalid upload response from server"));
        }
        return;
      }
      reject(new Error(xhr.responseText || `Upload failed (${xhr.status})`));
    };
    xhr.onerror = () => reject(new Error("Network error during upload"));
    xhr.send(formData);
  });
}

async function uploadViaFetch(file: UploadFile): Promise<UploadResponse> {
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

  const response = await fetch(`${API_BASE}/api/v1/reports`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Upload failed (${response.status})`);
  }

  return response.json() as Promise<UploadResponse>;
}

export type HealthResponse = {
  status: string;
  service: string;
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

export type ReportDetail = {
  reportId: number;
  filename: string;
  uploadedAt: string;
  extractionStatus: string;
  extractionError: string | null;
  biomarkerCount: number;
  biomarkers: Biomarker[];
};

export async function fetchHealth(): Promise<HealthResponse> {
  const response = await fetch(`${API_BASE}/api/v1/health`);
  if (!response.ok) {
    throw new Error(`Health check failed (${response.status})`);
  }
  return response.json();
}

export async function fetchReport(reportId: number): Promise<ReportDetail> {
  const response = await fetch(`${API_BASE}/api/v1/reports/${reportId}`);
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
    if (report.extractionStatus === "FAILED") {
      throw new Error(report.extractionError ?? "Extraction failed");
    }

    await new Promise((resolve) => setTimeout(resolve, intervalMs));
  }

  throw new Error("Extraction timed out");
}

export async function uploadReport(file: UploadFile): Promise<UploadResponse> {
  console.log("[phi] uploading", file.name, "via", Platform.OS, "to", `${API_BASE}/api/v1/reports`);

  try {
    const result =
      Platform.OS === "web" ? await uploadViaFetch(file) : await uploadViaXhr(file);
    console.log("[phi] upload ok", result.reportId, result.extractionStatus);
    return result;
  } catch (error) {
    console.error("[phi] upload failed", error);
    throw error;
  }
}

export { API_BASE };
