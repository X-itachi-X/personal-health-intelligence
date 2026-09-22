# Completed

Shipped features — newest first.

**North star:** [documentation/00-engineering-principles.md](../documentation/00-engineering-principles.md)

---

## 2026-09-18 — Phase 6c: Platform admin ops & free-tool comparison

| Feature | Area | Notes |
|---------|------|-------|
| `PlatformAccessService` | backend | `PHI_PLATFORM_ADMIN_EMAILS` — deployer-only access |
| App-wide ops APIs | backend | All testers' uploads/events; optional `familyId` filter |
| `GET /ops/reports/{id}/file` | backend | Download original PDF/image (when retained) |
| `GET /ops/reports/{id}/free-tools` | backend | Re-run PDFBox + Tesseract + rules side-by-side — **no Claude** |
| `DocumentTextExtractor.probeAllFreeTextTools` | backend | Independent free-tool probes for ops |
| Platform admin session flag | backend + mobile | `platformAdmin` on auth/me |
| Ops nav gating | mobile | **Platform Ops** visible only to platform admins |
| Free-tool comparison UI | mobile | Download original + compare tools on pipeline screen |
| Ops docs | docs | `documentation/21-ops-monitoring.md` updated |

**Config (tester phase):** `PHI_PLATFORM_ADMIN_EMAILS=admin@gmail.com`, `PHI_RETAIN_FILES_DAYS=7`.

**Use:** Distribute app to testers → observe all processing app-wide → download uploads → compare which free tool (PDFBox / OCR / rules) works before Claude.

---

## 2026-09-18 — Phase 6b: Ops UI (mobile)

| Feature | Area | Notes |
|---------|------|-------|
| Ops dashboard screen | mobile | Summary, failures, events, audit |
| Pipeline trace screen | mobile | Per-report timeline + tokens |
| Nav + Settings link | mobile | Platform admin only (updated in 6c) |
| Report detail link | mobile | "View pipeline trace" button |

---

## 2026-09-18 — Phase 6a: Pipeline telemetry & ops APIs

| Feature | Area | Notes |
|---------|------|-------|
| `extraction_events` table | backend | Per-step trail: text, rules, Claude, errors |
| `ExtractionTelemetryService` | backend | Instrumented full pipeline |
| Claude token capture | backend | `input_tokens` / `output_tokens` per AI call |
| Ops APIs (`/api/v1/ops/*`) | backend | Summary, events, errors, pipeline, audit |
| Ops docs | docs | `documentation/21-ops-monitoring.md` |

**Use:** Debug real uploads without relying only on tests — see OCR vs rules vs Claude path and token cost per report.

---

## 2026-09-18 — Phase 5a.5: OCR for scans & village reports

| Feature | Area | Notes |
|---------|------|-------|
| `DocumentTextExtractor` | backend | PDF text layer → OCR fallback → image OCR |
| `OcrTextExtractor` (Tesseract/tess4j) | backend | Scanned PDFs + JPEG/PNG uploads |
| `TextQualityAssessor` | backend | Detect thin PDF text layer, trigger OCR |
| `PHI_OCR_*` config | backend | Languages, DPI, tessdata path |
| Health check `ocrAvailable` | backend | Surfaces Tesseract install status |
| Village/scans doc | docs | `documentation/20-village-and-scanned-reports.md` |
| Mobile upload | mobile | Already accepts `image/*` + PDF |

**Requires:** `tesseract` + language packs on server (`dnf install tesseract tesseract-langpack-eng tesseract-langpack-hin`).

**Honest limit:** Handwriting not reliable — printed scans are the target; review UI planned.

---

## 2026-09-18 — Phase 5a: Rules-first extraction

| Feature | Area | Notes |
|---------|------|-------|
| `RuleBasedExtractor` + `RuleExtractionResult` | backend | Coverage score 0.0–1.0 |
| `OrangeHealthRuleExtractor` | backend | PDFBox text format; ~77 biomarkers without Claude |
| `RuleExtractionService` | backend | Picks best extractor; gates Claude via coverage |
| `PHI_CLAUDE_MIN_COVERAGE` (default 0.8) | backend | Skip Claude when rules sufficient |
| Pipeline integration | backend | Rules run before Claude in `runBiomarkerParsing` |
| Golden PDF tests (no API key) | backend | `OrangeHealthRuleExtractorTest`, `RuleExtractionServiceTest` |

**Verified:** Orange Health golden PDF → rules-only path at ≥80% coverage; Claude fallback when below threshold.

---

## 2026-09-18 — Phase 4b: Data sovereignty & staged ingest

| Feature | Area | Notes |
|---------|------|-------|
| Engineering principles doc | docs | `documentation/00-engineering-principles.md` |
| Documentation alignment | docs | README, data-flow, extraction, roadmap, product decisions |
| Cursor rule `engineering-principles.mdc` | docs | Always-on agent guidance |
| `content_hash` + upload dedup | backend | `409 duplicate_report` per person |
| Ephemeral PDF storage | backend | `PHI_RETAIN_FILES_DAYS=0`, purge after Phase 1 |
| Staged pipeline (Phase 1 → Phase 2) | backend | `TEXT_EXTRACTED` before Claude |
| `PHI_CLAUDE_AUTO_PARSE` config | backend | Manual token control in dev |
| `POST /api/v1/reports/{id}/parse` | backend | Claude from DB text only |
| Retry from `extracted_text` | backend | No PDF required |
| Duplicate upload UX | mobile | `DuplicateReportError` handling |
| Golden dataset expansion | data | 106 biomarkers from Orange Health PDF |

---

## 2026-09-18 — Phase 4: Analytics

| Feature | Area | Notes |
|---------|------|-------|
| DuckDB file + JDBC config | backend | `~/.phi/analytics.duckdb` |
| `AnalyticsSyncService` | backend | H2 → DuckDB on extraction + dev seed |
| Trend API | backend | `/api/v1/analytics/persons/{id}/trends` |
| Family compare API | backend | `/api/v1/analytics/family/{id}/compare` |
| Mobile trends screen | mobile | Sparklines + family compare |
| Golden dataset regression tests | backend | `GoldenDatasetRegressionTest`, `ReportExtractionGoldenTest` |
| Extraction retry endpoint | backend | `POST /api/v1/reports/{id}/retry` |
| Mobile design system | mobile | `components/ui/`, `DESIGN.md`, Ionicons + motion |

---

## 2026-09-17 — Phase 3a: Family platform

| Feature | Area | Notes |
|---------|------|-------|
| Accounts + JWT auth (email + password) | backend | `AuthController`, `JwtService` |
| Google token verifier | backend | Optional OAuth path |
| `families`, `memberships`, `invites`, `audit_events` | backend | Flyway `V4__family_platform.sql` |
| One family created per account | backend | Enforced in `FamilyService` |
| Extended `persons` profile fields | backend | DOB, sex, height, weight, etc. |
| Invite create + accept flow | backend | `InviteController`, `InviteService` |
| Upload scoped to `person_id` + ACL | backend | `AccessControlService` |
| Login / register screens | mobile | `login.tsx`, `register.tsx` |
| Family roster + feed UI | mobile | `family.tsx`, home feed |
| Family context + switch overlay | mobile | `FamilyContext.tsx` |
| Dev seed catalog | backend | Multi-family demo data |

---

## 2026-09-07 — Phase 2: Lab report extraction

| Feature | Area | Notes |
|---------|------|-------|
| PDF upload API (`POST /api/v1/reports`) | backend | Multipart upload, async extraction |
| PDF text extraction (PDFBox) | backend | Phase 1 — local, no API cost |
| Claude biomarker extraction | backend | Phase 2 — text from DB, once per report |
| Biomarker normalizer + DB persistence | backend | `biomarker_values` — open-ended canonical names |
| Report status API (`GET /api/v1/reports/{id}`) | backend | Full status lifecycle incl. `TEXT_EXTRACTED` |
| Mobile upload + poll UI | mobile | FormData fix for Android, 3 min poll window |
| H2 embedded DB (dev + prod profiles) | backend | Source of truth for all app data |
| Edge deploy scripts | infra | `setup.sh`, `deploy.sh`, `backup.sh`, openrc |
| Claude API setup docs | docs | README billing + `.env` workflow |
| Health endpoint extensions | backend | `claudeConfigured`, `claudeModel`, workspace flag |

**Verified:** Full body checkup PDF → 131 biomarkers end-to-end from phone.

_Evolved in Phase 4b to staged pipeline + ephemeral storage — see above._

---

## 2026-09-07 — Phase 1: Scaffold

| Feature | Area | Notes |
|---------|------|-------|
| Monorepo scaffold | all | Spring Boot + Expo + golden dataset layout |
| Initial schema + Flyway | backend | `persons`, `lab_reports` |
| Mobile home screen stub | mobile | Upload button, basic layout |
| Rules + skills placeholders | rules, skills | `cardiovascular.yaml`, `hematology.md` |
