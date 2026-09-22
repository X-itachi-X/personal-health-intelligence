# Backend Services

Spring Boot application entry: `com.phi.PhiApplication`

**Architecture:** [00-engineering-principles.md](./00-engineering-principles.md) — staged ingest, DB as truth.

---

## Service map

| Class | Package | Responsibility |
|-------|---------|----------------|
| `ReportIngestionService` | `ingestion` | Dedup hash, save ephemeral file, create `LabReport` |
| `ReportFileStorageService` | `storage` | SHA-256, purge PDF after Phase 1, retention job |
| `ReportExtractionService` | `extraction` | Phase 1 local text → H2; Phase 2 optional Claude |
| `PdfTextExtractor` | `extraction` | PDFBox text extraction (local, free) |
| `ClaudeExtractionClient` | `extraction` | Phase 2 — Anthropic API from `extracted_text` |
| `ClaudeResponseParser` | `extraction` | Parse Claude response JSON |
| `BiomarkerNormalizer` | `extraction` | Alias map → canonical biomarker IDs |
| `AnalyticsSyncService` | `analytics` | H2 → DuckDB sync after `COMPLETED` |
| `AccessControlService` | `access` | Family ACL, upload/read permissions |
| `AuthService` | `auth` | Register, login, JWT |

### Placeholders (not implemented)

| Class | Package | Status |
|-------|---------|--------|
| `IngestionModule` | `ingestion` | Empty utility class |
| `ReasoningModule` | `reasoning` | Empty — planned rules engine |

---

## ReportIngestionService

**Method:** `ingest(MultipartFile, account, familyId, personId) → LabReport`

| Step | Action |
|------|--------|
| 1 | ACL check |
| 2 | Save file to `phi.storage.reports-dir` |
| 3 | SHA-256 `content_hash` |
| 4 | Duplicate check per person → `DuplicateReportException` (409) |
| 5 | Create `LabReport` (`PENDING`) |
| 6 | Audit log |

---

## ReportExtractionService

Uses `@Lazy` self-injection for `@Transactional` + `@Async` proxy boundaries.

### `extractAsync(reportId)` — full pipeline entry

1. `extractAndPersistLocalText()` — Phase 1
2. If no Claude key → `finalizeTextOnly()`
3. If `PHI_CLAUDE_AUTO_PARSE` → `runClaudeParsing()` — Phase 2

### `extractAndPersistLocalText(reportId) → boolean` — Phase 1

Transactional. PDFBox → `markTextExtracted()` → purge PDF.

### `runClaudeParsing(reportId)` — Phase 2

`@Async`. Reads `extracted_text` from H2 → Claude → `completeExtraction()`.

### `parseWithClaudeAsync(reportId)` — manual Phase 2

Validates + schedules `runClaudeParsing()`. Used by `POST .../parse` and retry when text exists.

### `retryExtraction(reportId)`

If `extracted_text` in DB → Phase 2 only. Else full pipeline.

### `completeExtraction(reportId, text, biomarkers)`

Transactional. Save `biomarker_values`, `COMPLETED`, DuckDB sync.

---

## ReportFileStorageService

| Method | Purpose |
|--------|---------|
| `hashContent` / `hashFile` | SHA-256 for dedup |
| `purgeStoredFile` | Delete file, null `storage_path` |
| `purgeAfterExtractionIfDue` | Immediate if `retain-files-days=0` |
| `purgeExpiredFiles` | Nightly job when retention &gt; 0 |

---

## PdfTextExtractor

Apache PDFBox 3.0.4 — **local only, no API cost**.

---

## ClaudeExtractionClient

**Phase 2 only.** Never receives PDF bytes — only plain text from H2.

---

## Configuration beans

| Class | Prefix |
|-------|--------|
| `PhiProperties` | `phi.*` |
| `AsyncConfig` | Thread pool `phi-extract-*` |
| `DuckDbConfig` | `phi.analytics.duckdb-path` |
| `SecurityConfig` | JWT filter chain |

See [09-configuration.md](./09-configuration.md).
