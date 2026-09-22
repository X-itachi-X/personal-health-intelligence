# Backlog

Planned features — not started yet. Pull into [CURRENT.md](./CURRENT.md) when a sprint begins.

**North star:** [documentation/00-engineering-principles.md](../documentation/00-engineering-principles.md)  
**Product vision:** [documentation/16-product-vision-family-and-modes.md](../documentation/16-product-vision-family-and-modes.md)  
**Decisions log:** [documentation/19-product-decisions.md](../documentation/19-product-decisions.md)

---

## Phase 5 — Engineered intelligence ✅ **DONE**

See [COMPLETED.md](./COMPLETED.md) and [sprint-05-engineered-intelligence.md](./sprint-05-engineered-intelligence.md).

| Sub-sprint | Summary |
|------------|---------|
| 5a | Rules-first lab extraction |
| 5b | YAML clinical rules + findings API + mobile |
| 5c | SQL insights, family snapshot, med correlation |
| 5d | Agent SQL tool endpoints (chat foundation) |
| 5e | Prescription ingest + medication log |

---

## Phase 3b — Basic vs advanced UX 🔶 **PARTIAL**

| Feature | Area | Status | Notes |
|---------|------|--------|-------|
| UI mode toggle (basic / advanced) | mobile + backend | partial | Settings exists; verify `PATCH /auth/me/mode` |
| Basic home: family feed | mobile + backend | done | `fetchFamilyFeed` on home |
| Report soft-delete | backend + mobile | done | `DELETE /api/v1/reports/{id}` |
| Audit log (upload for X) | backend | done | `audit_events` |
| Advanced: upload for other members | mobile | done | Person picker on upload |
| Report history screen | mobile | done | `reports.tsx` |
| Google sign-in | backend + mobile | partial | Backend verifier exists |
| PIN for advanced mode | mobile | planned | Optional guard — Phase 6 |
| ~~AI explanation layer~~ | — | **deferred** | Replace with rules + SQL insights (Phase 5) |
| ~~Reasoning via Claude~~ | — | **rejected** | Use `rules/*.yaml` deterministically |

---

## Phase 6 — Ops & monitoring ✅ **DONE**

See [COMPLETED.md](./COMPLETED.md#2026-09-18--phase-6c-platform-admin-ops--free-tool-comparison).

---

## Phase 6 — Clinical context beyond labs 🔶 **PARTIAL**

| Sub-sprint | Status | Notes |
|------------|--------|-------|
| 6a — Imaging report ingest | ✅ | USG/X-ray/MRI/CT report text → `imaging_studies` |
| 6b — More imaging golden fixtures | ⬜ | Hospital-specific report formats |
| 6c — Cross-modal YAML rules | ⬜ | e.g. fatty liver USG + ALT in `rules/` |

---

## Phase 7 — Polish & deploy

| Feature | Area | Notes |
|---------|------|-------|
| Image/OCR path (Phase 1 local) | backend | ✅ Done — see Phase 5a.5 |
| Chat UI over agent tools | mobile/web | Phase 5d tools exist; UI next |
| More prescription formats / golden Rx fixtures | backend | Improve regex + golden tests |
| Dependent profiles (no phone) | backend + mobile | Person without account |
| Reminders (re-test intervals) | mobile | DB-driven schedules, no Claude |
| Optional web dashboard | web | Advanced analytics in browser |
| Edge-server production deploy | infra | `infrastructure/edge/setup.sh` |
| PIN for advanced mode | mobile | Optional guard |
| Haiku option for Phase 2 parse | backend | Cost reduction at ingest only |

---

## Ideas (must align with principles)

| Idea | Verdict | Notes |
|------|---------|-------|
| Haiku for Phase 2 biomarker parse | ✅ Aligns | Ingest cost reduction only |
| Local OCR for image uploads | ✅ Aligns | Phase 1, no API |
| Export summary PDF | ⬜ Later | Generated from DB rows, not stored PDFs |
| Multi-language UI | ⬜ Later | Copy translation, not LLM-first |
| ~~ChatGPT-style “ask about my PDF”~~ | ❌ Reject | Generic chat — not our moat |
| ~~Store PDFs long-term~~ | ❌ Reject | Ephemeral ingest only |
| ~~Claude on every trend question~~ | ❌ Reject | DuckDB answers this |

---

## Completed phases (see COMPLETED.md)

Phases 1, 2, 3a, 4, 4b, 5, 6 are done. Do not re-add to backlog.
