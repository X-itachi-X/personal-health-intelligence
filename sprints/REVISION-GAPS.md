# Revision gaps — work one by one

Tracked from full product revision (2026-09-19).  
**North star:** [documentation/00-engineering-principles.md](../documentation/00-engineering-principles.md)

Status: `⬜` open · `🔶` in progress · `✅` done

---

## P0 — Broken or confusing user paths

| # | Gap | Area | Status |
|---|-----|------|--------|
| P0-1 | Home `handleSubmitRequiredReportDate` does not redirect to report detail when status is `AWAITING_MEDICATION_CONFIRMATION` or `AWAITING_IMAGING_CONFIRMATION` | mobile | ✅ |
| P0-2 | Report detail shows biomarker chrome (count, rows) for completed prescription/imaging reports | mobile | ✅ |
| P0-3 | Upload copy wrong for non-lab: confirm sheet title, progress steps, photo date hints | mobile | ✅ |

---

## P1 — Completeness (backend exists, UI missing)

| # | Gap | Area | Status |
|---|-----|------|--------|
| P1-1 | Confirm screens read-only — cannot edit meds/imaging before save | mobile | ✅ |
| P1-2 | Medication end/delete not wired (`POST .../end`, `DELETE`) | mobile | ✅ |
| P1-3 | No manual Claude parse button (`POST /reports/{id}/parse`) when `TEXT_EXTRACTED` | mobile | ✅ |
| P1-4 | Reports list missing `documentType` + friendly status labels | backend + mobile | ✅ |
| P1-5 | Dedup ignores `document_type` — same file cannot upload as different doc types | backend | ✅ |
| P1-6 | Manual medication add does not expose course type / schedule | mobile | ✅ |
| P1-7 | Family feed shows raw `extractionStatus` enum strings | mobile | ✅ |

---

## P2 — Quality & moat

| # | Gap | Area | Status |
|---|-----|------|--------|
| P2-1 | Golden fixtures for prescription reports | backend + data | ✅ |
| P2-2 | Golden fixtures for imaging (USG/X-ray) reports | backend + data | ✅ |
| P2-3 | Cross-modal rules in `rules/*.yaml` (not only Java heuristics) | backend | ✅ |
| P2-4 | Imaging study detail screen + link to source report | mobile | ✅ |
| P2-5 | Home health summary ignores prescription/imaging context | mobile | ✅ |
| P2-6 | `features.ts` / Features screen stale | mobile | ✅ |
| P2-7 | `documentation/15-roadmap-and-gaps.md` stale vs built reality | docs | ✅ |
| P2-8 | No HTTP/controller tests (auth, confirm, ACL) | backend | ✅ |

---

## P3 — Phase 7 (later, deliberate)

| # | Gap | Area | Status |
|---|-----|------|--------|
| P3-1 | Chat UI over agent tools | mobile/web | ✅ |
| P3-2 | Dependent profiles without phone | backend + mobile | ✅ |
| P3-3 | Re-test reminders (DB-driven) | mobile | ✅ |
| P3-4 | Web dashboard | web | ✅ |
| P3-5 | Edge production deploy / TLS / backups | infra | ✅ |
| P3-6 | PIN for advanced mode | mobile | ⬜ |
| P3-7 | Google sign-in production-ready | mobile | ⬜ |
| P3-8 | Family rename UI (`PATCH /family/{id}`) | mobile | ⬜ |
| P3-9 | Haiku option for ingest parse | backend | ⬜ |
| P3-10 | Hindi / regional UI | mobile | ⬜ |

---

## Work order

1. P0-1 → P0-2 → P0-3  
2. P1-1 … P1-7  
3. P2-* as fixtures become available  
4. P3-* per backlog Phase 7  

Update status in this file as each item is completed.
