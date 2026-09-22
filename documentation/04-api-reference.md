# API Reference

Base URL: `http://localhost:8080` (dev) or your edge server domain (prod).

All report endpoints are currently **unauthenticated**. See [10-security.md](./10-security.md).

---

## `GET /api/v1/health`

Health check with Claude configuration diagnostics.

**Controller:** `HealthController`

**Response:** `200 OK`

```json
{
  "status": "ok",
  "service": "personal-health-intelligence",
  "claudeConfigured": true,
  "claudeModel": "claude-sonnet-4-6",
  "claudeWorkspaceConfigured": true
}
```

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | Always `"ok"` when server is running |
| `service` | string | Service identifier |
| `claudeConfigured` | boolean | `CLAUDE_API_KEY` is set and non-blank |
| `claudeModel` | string | Active model from `phi.claude.model` |
| `claudeWorkspaceConfigured` | boolean | `CLAUDE_WORKSPACE_ID` is set |

---

## `POST /api/v1/reports`

Upload a lab report file for extraction.

**Controller:** `ReportController`

**Content-Type:** `multipart/form-data`

**Request:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | file | Yes | PDF (preferred) or image — max 25 MB |

**Example (curl):**

```bash
curl -X POST http://localhost:8080/api/v1/reports \
  -F "file=@/path/to/report.pdf"
```

**Response:** `200 OK`

```json
{
  "reportId": 10,
  "filename": "report2.pdf",
  "uploadedAt": "2026-09-07T12:40:38.255093Z",
  "extractionStatus": "PENDING",
  "message": "Report received. Extraction started."
}
```

**Side effects:**

1. PDF saved to `phi.storage.reports-dir`
2. `lab_reports` row created
3. Async extraction started

**Errors:** No global exception handler — invalid input may return Spring default `400`/`500` with stack trace in dev.

---

## `GET /api/v1/reports/{reportId}`

Get report status and extracted biomarkers.

**Controller:** `ReportQueryController`

**Path parameter:** `reportId` (Long)

**Response:** `200 OK`

```json
{
  "reportId": 10,
  "filename": "report2.pdf",
  "uploadedAt": "2026-09-07T12:40:38.255093Z",
  "extractionStatus": "COMPLETED",
  "extractionError": null,
  "biomarkerCount": 131,
  "biomarkers": [
    {
      "canonical": "alt",
      "testName": "ALT (SGPT)",
      "value": 57.0,
      "textValue": null,
      "unit": "U/L",
      "referenceRange": "0 - 40",
      "confidence": 0.99
    }
  ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `extractionStatus` | string | `PENDING`, `PROCESSING`, `COMPLETED`, `TEXT_ONLY`, `FAILED` |
| `extractionError` | string \| null | Error message when `FAILED` |
| `biomarkerCount` | int | Length of `biomarkers` array |
| `biomarkers[].canonical` | string | Normalized biomarker ID |
| `biomarkers[].testName` | string | Original name from report |
| `biomarkers[].value` | number \| null | Numeric result |
| `biomarkers[].textValue` | string \| null | Non-numeric result (e.g. "Negative") |
| `biomarkers[].unit` | string \| null | Measurement unit |
| `biomarkers[].referenceRange` | string \| null | Lab reference range as printed |
| `biomarkers[].confidence` | number \| null | Extraction confidence 0.0–1.0 |

**Errors:**

- `404` / `500` with `IllegalArgumentException: Report not found: {id}` if ID does not exist

Biomarkers are sorted by `canonical_name` ascending.

---

## Actuator endpoints

Configured in `application.yml`:

| Endpoint | Auth | Description |
|----------|------|-------------|
| `GET /actuator/health` | Public | Spring Boot health |
| `GET /actuator/info` | Authenticated* | App info (blocked by default security) |

*No login mechanism configured — effectively inaccessible except `/actuator/health`.

---

## Dev-only: H2 Console

**URL:** `http://localhost:8080/h2-console` (dev profile only)

| Setting | Value |
|---------|-------|
| JDBC URL | `jdbc:h2:file:~/.phi/h2db` |
| User | `sa` |
| Password | (empty) |

Disabled in production (`application-prod.yml`).

---

## Extraction status enum

| Value | Meaning |
|-------|---------|
| `PENDING` | Uploaded, extraction not started |
| `PROCESSING` | Extraction in progress |
| `COMPLETED` | Biomarkers saved successfully |
| `TEXT_ONLY` | PDF text saved; Claude not configured |
| `FAILED` | Error stored in `extractionError` |

---

### `POST /api/v1/reports/{reportId}/retry`

Re-run extraction on an existing upload without re-uploading the file.

**Auth:** Required — caller must be able to read the report.

**Responses:**

| Status | Meaning |
|--------|---------|
| `202 Accepted` | Retry queued; status becomes `PROCESSING` |
| `404` | Report not found or soft-deleted |
| `403` | No read access |
| `409` | Extraction already in progress |

```json
{
  "reportId": 42,
  "extractionStatus": "PROCESSING",
  "message": "Extraction retry started"
}
```

---

## Endpoints not yet implemented

| Planned endpoint | Purpose |
|------------------|---------|
| `GET /api/v1/reports/{id}/summary` | AI explanation + rule findings |
