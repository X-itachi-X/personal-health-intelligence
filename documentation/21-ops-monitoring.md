# Platform Ops & Pipeline Monitoring

**For the app owner / deployer only** — not for family users or testers.

See how **all testers** across the whole app are processing reports, where AI ran, and token spend.

_Last updated: 2026-09-18_

---

## Who can access

| Role | Sees ops? |
|------|-----------|
| **Platform admin** (you) | Yes — all users, all families |
| **Testers / family users** | No — they use the app normally |

Configure platform admins on the server:

```bash
PHI_PLATFORM_ADMIN_EMAILS=you@example.com,co-founder@example.com
```

Restart backend after changing. Log in with that email — `/api/v1/auth/me` returns `"platformAdmin": true`.

---

## Why

When a tester complains *"my upload failed"*, you open **Platform Ops**, find their report, and see the exact pipeline step that broke — without SSH or log grep.

---

## What is logged

Every report gets a **step-by-step trail** in `extraction_events`:

| Event type | Meaning |
|------------|---------|
| `pipeline.started` | Upload received, extraction began |
| `text.pdf_layer` | Digital PDF text extracted (PDFBox) |
| `text.ocr_pdf` | Scanned PDF — Tesseract OCR |
| `text.ocr_image` | Photo upload — Tesseract OCR |
| `text.ai_vision` | Claude vision — **last resort** text extraction |
| `text.stored_retry` | Retry used DB text (file already purged) |
| `parse.rules` | Rules attempted — coverage % logged |
| `parse.claude` | Claude biomarker parse — **last resort** + **tokens** |
| `parse.skipped` | Claude skipped (not configured / auto-parse off) |
| `file.purged` | Ingest file deleted after text saved |
| `pipeline.completed` | Biomarkers saved |
| `pipeline.failed` | Error — message stored |

Each row can include: `duration_ms`, `char_count`, `biomarker_count`, `coverage`, `input_tokens`, `output_tokens`, `model`.

---

## API (platform admin only)

| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/ops/summary?days=7` | App-wide token totals, AI calls, failures |
| `GET /api/v1/ops/reports?limit=50` | Recent uploads from **all users** |
| `GET /api/v1/ops/events?limit=50` | All pipeline events (optional `familyId` filter) |
| `GET /api/v1/ops/errors?days=7&limit=30` | All failures |
| `GET /api/v1/ops/reports/{id}/pipeline` | Tools, **full extracted text**, timeline |
| `GET /api/v1/ops/reports/{id}/file` | Download original PDF/image (if retained) |
| `GET /api/v1/ops/reports/{id}/free-tools` | **Re-run all free tools** side-by-side (no Claude) |
| `GET /api/v1/ops/audit?limit=50` | All audit events (optional `familyId` filter) |

---

## Free-tool comparison (testing phase)

During tester rollout, set:

```bash
PHI_RETAIN_FILES_DAYS=7   # keep uploads on disk for ops download
```

Then for any report, call **free-tools** (or use **Run free-tool comparison** in mobile ops):

| Tool | What it tests |
|------|----------------|
| `text.pdf_layer` | PDFBox — digital PDF text layer |
| `text.ocr_pdf` | Tesseract — scanned PDF |
| `text.ocr_image` | Tesseract — phone photo |
| `parse.rules` | Template/rules parse on each text output |

Each result includes: status (`success` / `warning` / `error`), char count, extracted text preview, and biomarkers found by rules. **Claude is not called** — this is for evaluating free local tools only.

If the original file was purged (`PHI_RETAIN_FILES_DAYS=0`), comparison still runs rules on stored DB text, but OCR/PDF layer cannot be re-run.

---

## Example: debugging a failed village scan

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/ops/reports/42/pipeline
```

Response shows:

1. `text.ocr_image` — 45 chars, `warning` (low quality)
2. `parse.rules` — coverage 0%, insufficient
3. `parse.claude` — 6200 input / 1800 output tokens
4. `pipeline.completed` — 12 biomarkers

You immediately see: OCR was weak, rules didn't match, Claude was needed, cost was ~8k tokens.

---

## Mobile UI

**Platform admin only** (testers never see this):

- Menu → **Platform Ops** — all testers' uploads, failures, events, audit
- Tap any upload → download original, **compare free tools**, pipeline trace
- Settings → **Open platform console** (if your email is in `PHI_PLATFORM_ADMIN_EMAILS`)

---

## Related

- [07-extraction-pipeline.md](./07-extraction-pipeline.md) — pipeline stages
- [00-engineering-principles.md](./00-engineering-principles.md) — AI last resort
