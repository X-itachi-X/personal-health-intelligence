# Extraction Pipeline

Staged pipeline: **local text → database checkpoint → optional Claude parse**.

Claude is the **ingest boundary parser**, not the application brain. See [00-engineering-principles.md](./00-engineering-principles.md).

---

## Overview

```
PDF / image / scan (ephemeral)
    ↓ Phase 1 — local, free
DocumentTextExtractor
    ├─ digital PDF → PDFBox text layer
    ├─ scanned PDF / thin text → Tesseract OCR (render pages)
    └─ photo (JPEG, PNG, …) → Tesseract OCR
    ↓
extracted_text in H2  (status: TEXT_EXTRACTED)
    ↓ delete PDF (default)
    ↓ Phase 2 — Claude, once, from DB text
Claude Messages API
    ↓ JSON array
ClaudeResponseParser + BiomarkerNormalizer
    ↓
biomarker_values in H2  (status: COMPLETED)
    ↓
DuckDB analytics sync
```

**Typical duration:** Phase 1 seconds; Phase 2 ~1–3 minutes for large panels.

---

## Phase 1: Local text extraction

**Classes:** `DocumentTextExtractor`, `PdfTextExtractor`, `OcrTextExtractor`, `TextQualityAssessor`, `ReportExtractionService.extractAndPersistLocalText()`

```java
DocumentTextExtractor.extract(filePath)
  → try PDF text layer (PDFBox)
  → if thin/missing text → Tesseract OCR on rendered pages
  → if image upload → Tesseract OCR directly
→ report.markTextExtracted(text)
→ fileStorageService.purgeAfterExtractionIfDue(report)
```

| Input | Method | Output |
|-------|--------|--------|
| Digital PDF (text layer) | `PDF_TEXT_LAYER` | Concatenated text from all pages |
| Scanned PDF (CamScanner, Adobe Scan) | `OCR_PDF` | OCR text from rendered pages (300 DPI) |
| Photo / image (JPEG, PNG, …) | `OCR_IMAGE` | OCR text from image |
| Plain `.txt` | `PLAIN_TEXT` | Raw file contents (dev/tests) |

**Primary users:** village labs, phone photos, scanner apps — not fancy digital PDFs. See [20-village-and-scanned-reports.md](./20-village-and-scanned-reports.md).

**Server requirement:** Tesseract must be installed (`PHI_OCR_ENABLED=true`). Health check: `ocrAvailable` on `/api/v1/health`.

**Honest limit:** Tesseract is for **printed** text. Handwritten slips may need family review (planned).

**Checkpoint:** `extracted_text` CLOB in `lab_reports` **before** any Claude call.

**Dedup:** `content_hash` (SHA-256) checked at upload — duplicate file for same person → `409`, no Claude spend.

If text is blank → `FAILED: No text could be extracted from the document`.

### Phase 1.5: Rules-first extraction ✅

**Classes:** `RuleExtractionService`, `OrangeHealthRuleExtractor`

Before calling Claude, attempt template/regex extraction for known lab formats.

| Step | Action |
|------|--------|
| 1 | Detect lab format (e.g. Orange Health via `orangehealth.in`) |
| 2 | Parse name / value / reference from PDFBox text layout |
| 3 | Compute **coverage** = biomarkers found ÷ parseable slots |
| 4 | If coverage ≥ `PHI_CLAUDE_MIN_COVERAGE` (default **0.8**) → **skip Claude**, save to H2 |
| 5 | Else → Claude fallback on same `extracted_text` |

Config: `PHI_CLAUDE_MIN_COVERAGE=0.8`

**Verified:** Orange Health golden PDF extracts ~77 biomarkers without API key at ≥80% coverage.

---

## Phase 2: Claude biomarker parsing

**Only when:**

- `CLAUDE_API_KEY` is set
- `PHI_CLAUDE_AUTO_PARSE=true` (default), or manual `POST /api/v1/reports/{id}/parse`

**Classes:** `ClaudeExtractionClient`, `ClaudeResponseParser`, `ReportExtractionService.runClaudeParsing()`

**Input:** `extracted_text` read from H2 — **never PDF bytes**.

### Request body

```json
{
  "model": "claude-sonnet-4-6",
  "max_tokens": 16384,
  "system": "<extraction instructions>",
  "messages": [
    {
      "role": "user",
      "content": "Extract all lab test results from this report text:\n\n<text>"
    }
  ]
}
```

### System prompt requirements

Claude must return **only** a JSON array. Each object:

| Field | Type | Description |
|-------|------|-------------|
| `testName` | string | Original name from report |
| `canonical` | string | snake_case ID, e.g. `vitamin_d` |
| `value` | number \| null | Numeric result |
| `textValue` | string \| null | Non-numeric, e.g. `"Negative"` |
| `unit` | string | Measurement unit |
| `referenceRange` | string | As printed on report |
| `confidence` | number | 0.0–1.0 |
| `sourcePage` | number \| null | Page if known |

### Cost drivers

- Input tokens: ~18k for large reports (50k char text)
- Output tokens: ~6k–13k for full panels
- Model: Sonnet 4.6 (~₹5–7 per report)

**Mitigation:** dedup, rules-first (planned), `PHI_CLAUDE_AUTO_PARSE=false` in dev, never re-parse `COMPLETED` reports.

---

## Response parsing

**Class:** `ClaudeResponseParser`

Handles markdown fences, concatenated content blocks, substring `[` … `]`, Jackson deserialization.

If `stop_reason == "max_tokens"` → fail with message to increase `max_tokens` (currently 16384).

---

## Normalization

**Class:** `BiomarkerNormalizer`

Claude may supply `canonical`; otherwise alias map maps raw names → snake_case IDs.

Open-ended storage: unknown tests become new `canonical_name` rows — no schema change per new lab test.

---

## Persistence

**After Phase 2 only:**

1. `DELETE FROM biomarker_values WHERE lab_report_id = ?`
2. Insert one row per biomarker
3. `lab_reports.extraction_status = COMPLETED`
4. `lab_reports.extracted_text` retained (same text from Phase 1)
5. `AnalyticsSyncService.syncReport()` → DuckDB

---

## Status reference

| Status | Phase | PDF on disk | `extracted_text` | `biomarker_values` |
|--------|-------|-------------|------------------|-------------------|
| `PENDING` | — | Yes | — | — |
| `PROCESSING` | 1 or 2 | Maybe | Maybe | — |
| `TEXT_EXTRACTED` | 1 done | No (default) | Yes | — |
| `COMPLETED` | 2 done | No | Yes | Yes |
| `TEXT_ONLY` | 1 done, no key | No | Yes | — |
| `FAILED` | Either | Maybe | Maybe | — |

---

## Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `CLAUDE_API_KEY` | — | Required for Phase 2 |
| `CLAUDE_WORKSPACE_ID` | — | Often required for API keys |
| `CLAUDE_MODEL` | `claude-sonnet-4-6` | Model override |
| `PHI_CLAUDE_AUTO_PARSE` | `true` | Auto-run Phase 2 after Phase 1 |
| `PHI_RETAIN_FILES_DAYS` | `0` | Delete PDF after Phase 1 |

---

## API endpoints

| Endpoint | Action |
|----------|--------|
| `POST /api/v1/reports` | Upload → starts Phase 1 (+ Phase 2 if auto-parse) |
| `POST /api/v1/reports/{id}/parse` | Phase 2 only (from `extracted_text`) |
| `POST /api/v1/reports/{id}/retry` | Phase 2 if text exists; else full pipeline |

---

## What Claude must never do in PHI

| Anti-pattern | Correct approach |
|--------------|------------------|
| Answer trend questions | DuckDB `GET /analytics/trends` |
| Re-read PDF on retry | Read `extracted_text` from H2 |
| Parse duplicate upload | Block at hash check |
| Power the whole chatbot | SQL tools + small JSON context |

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `TEXT_EXTRACTED` stuck | `PHI_CLAUDE_AUTO_PARSE=false` | `POST .../parse` or enable auto-parse |
| `409 duplicate_report` | Same file re-uploaded | Use existing `existingReportId` |
| Workspace header error | Missing `CLAUDE_WORKSPACE_ID` | Add to `.env` |
| Phase 2 failed, text saved | Claude/parse error | `POST .../retry` (no PDF needed) |
| 0 biomarkers, COMPLETED | Empty Claude array | Check logs, retry parse |

---

## Future improvements (aligned with principles)

1. **Rules-first extraction** — Claude only for gaps (priority)
2. Chunk large reports to reduce tokens
3. Structured output / tool use for guaranteed JSON schema
4. Haiku for Phase 2 cost reduction
5. OCR path for images (Phase 1, still local)
6. Golden dataset regression for extraction accuracy ✅ (done)
