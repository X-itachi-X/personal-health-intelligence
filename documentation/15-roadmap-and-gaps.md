# Roadmap & Known Gaps

What is built, what is planned, and known inconsistencies.

**Direction:** [00-engineering-principles.md](./00-engineering-principles.md) — engineered platform, not chat wrapper.

---

## Built ✅

| Capability | Details |
|------------|---------|
| Monorepo scaffold | Backend + mobile + rules + skills + data |
| Staged extraction | Phase 1 local text → H2; Phase 2 rules → optional Claude from DB text |
| Ephemeral PDF storage | Purge after local extract; `content_hash` dedup per document type |
| Three document types | Lab reports, prescriptions, imaging reports — shared ingest pipeline |
| Claude at ingest boundary only | Biomarkers, meds, imaging — once per unique report |
| H2 persistence | Persons, reports, `extracted_text`, biomarker values, meds, imaging |
| DuckDB analytics | Trends, compare, changes — synced from H2 |
| Family platform | JWT auth, families, invites, per-person upload, audit |
| Mobile app | Upload (3 types), confirm flows, reports, trends, family, settings |
| YAML clinical rules | `rules/*.yaml` — deterministic findings (cardiovascular, metabolic, etc.) |
| Cross-modal rules | `rules/cross_modal/*.yaml` — imaging + lab correlation insights |
| Golden dataset regression | Lab (106+ biomarkers), prescription parser, imaging parser fixtures |
| Prescription ingest | Extract → editable confirm → medication log |
| Imaging ingest | Extract → editable confirm → imaging timeline + study detail |
| SQL insight cards | Home insights: rules, changes, med/imaging context, trends |
| Agent tools API | `/api/v1/agent/tools/*` — no chat UI yet |
| Extraction retry / parse | `POST .../retry`, `POST .../parse` |
| Edge deploy scripts | setup, deploy, backup, openrc |

---

## In progress / next

See [../sprints/REVISION-GAPS.md](../sprints/REVISION-GAPS.md) for the active gap list.

| Priority | Item | Status |
|----------|------|--------|
| **P3** | Chat UI over agent tools | Done (mobile Ask screen) |
| **P3** | Dependent profiles without phone | Planned |
| **P3** | Re-test reminders | Planned |
| **P3** | Web dashboard | Planned |
| **P3** | Production hardening (TLS, backups) | Done |

---

## Planned (Phase 7+) ⬜

| Item | Notes |
|------|-------|
| Chatbot with SQL tools | Small JSON context per question, not PDF dumps |
| Hindi / regional language | UI copy, not LLM-first |
| Haiku option for ingest | Token savings on bulk parse |
| More golden hospital PDFs | Real prescription + USG fixtures when available |

---

## Architecture gaps (remaining)

| Gap | Impact | Priority |
|-----|--------|----------|
| Chat UI not wired | Agent tools exist but no mobile/web surface | Medium |
| Medication cross-modal rules in YAML | Med correlation still partly Java maps | Low |
| HTTP test coverage thin | One controller test; expand ACL/confirm | Low |
| `report_date`, `lab_name` in SQL but not all JPA paths | Cannot set from some code paths | Low |

---

## Deliberate non-goals

| We are **not** building | Because |
|-------------------------|---------|
| PDF archive / document manager | DB is source of truth |
| Claude on every API read | SQL/DuckDB answers structured queries |
| Generic health chat | ChatGPT/Gemini already do that |
| Re-parse on duplicate upload | Dedup + `COMPLETED` guard per document type |

---

## Technical debt

| Item | Notes |
|------|-------|
| Expand controller/integration tests | Auth, confirm flows, ACL |
| Real hospital prescription/USG PDFs in golden set | Synthetic fixtures exist; add PDFs when shared |
