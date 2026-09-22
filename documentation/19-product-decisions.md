# Product Decisions Log

Recorded answers from product Q&A — **2026-09-17**.

Use this as source of truth when implementing. Supersedes conflicting notes in earlier drafts.

---

## Authentication

| Decision | Choice |
|----------|--------|
| **Sign-in method (v1)** | **Email + password** — easiest to implement, no per-login cost |
| **Phone OTP** | Defer — SMS providers charge per message (not free) |
| **Google / Apple** | Optional later — free OAuth, more integration work |
| **One phone = one account** | **Yes, strictly** — one device binding per account (enforce on login/register) |
| **Kids / elders without email** | **Profile only, no account** — reduce friction; account only when they need the app |

### Why email + password for v1

- Spring Security + BCrypt — standard, no third-party fees
- No SMS gateway setup
- Google/Apple can be added in Phase 5 without breaking email users

---

## Family model (updated)

**Not** flat egalitarian peers only. **Multi-family membership** with **per-family roles**.

### Key rules

| Rule | Decision |
|------|----------|
| Families per creator | **One person can create only ONE family** (as creator/owner) |
| Membership | **One person can belong to multiple families** (e.g. Person 4 in Family A and Family B) |
| Roles per family | **Admin** (creator by default), can assign **Maintainer** or other roles |
| Invite | Admin/maintainer (TBD exact permission) invites into **their** family |
| Leave family | **Yes** — member can leave |
| Delete data on leave | **Separate setting** — user can delete their data when leaving (not automatic) |

### Example (from product owner)

```
Family A (created by A): members 1, 2, 3, 4
Family B (created by B): members 4, 5, 6, 7   ← person 4 in BOTH families
Family C (created by 5): members 5, C, D      ← 5 also created a family
```

Person 4 sees reports/context per family. Upload and visibility scoped by **which family context** user is viewing (UI detail TBD).

### Relationship field

**Fixed set:** spouse, parent, child, sibling, self, other (extend list in code as enum).

---

## Basic vs advanced mode

| Decision | Choice |
|----------|--------|
| Advanced toggle | **Simple switch** (no PIN for now) |
| Default for new signups | **Basic** until user opts in |
| Basic mode visibility | **Own data** + simple **family health feed** (e.g. “Mom’s report is ready”) — easy to read, not full clinical detail for others |
| Advanced mode | Full family management, upload for others, web + mobile dashboards, all detail |

---

## Privacy & reports

| Decision | Choice |
|----------|--------|
| Upload for another person | **Auto-visible** to that person — **no approval** |
| Wrong report uploaded | **Delete report** option (uploader or admin — implement with audit) |
| Hide sensitive tests in basic mode | **No** — show everything equally |
| Audit log | **Yes** — e.g. “X uploaded report for Y at Z” |

---

## Onboarding profile fields

| Field | Required? |
|-------|-----------|
| Name | Yes |
| Date of birth | Yes |
| Sex | Yes |
| Relationship (to family context) | Yes |
| Height, weight, blood group, medications, conditions, etc. | **Optional** — collect as much as possible, none blocking |

Invite code expiry: **7 days** (default from earlier doc unless changed).

---

## Hosting & scale

| Decision | Choice |
|----------|--------|
| Deployment | **One server, many families** (multi-tenant) |
| Data residency | **Your local / edge server** — data stays on hardware you control |
| Claude API | Still cloud (text only) — only analytics DB + PDFs + H2 on your server |

### Scale estimate (single 4 GiB edge VM, H2, current architecture)

| Metric | Comfortable | Stretch |
|--------|-------------|---------|
| Families | 50–200 | 500+ with tuning |
| Total persons (profiles) | 500–2,000 | 10,000+ |
| Lab reports / year | 2,000–10,000 | 50,000+ |
| Concurrent users | ~10–20 | ~50 |

**Bottlenecks:** Claude API cost/latency per upload, disk for PDFs, JVM RAM — not H2 row count at family scale.

**When to upgrade:** Move H2 → PostgreSQL, separate object storage for PDFs, queue for extraction — if you exceed ~200 active families or need HA.

---

## Dashboards & export

| Decision | Choice |
|----------|--------|
| Dashboards | **Mobile + web** for advanced users |
| Dashboard content | **All available detail** — trends, out-of-range, family compare, cost over time, etc. |
| Export PDF/CSV | **No** for now |

---

## AI usage (Claude is not the product)

**Recorded 2026-09-18** — PHI is an **engineered health data platform**, not a ChatGPT/Gemini chat wrapper or PDF dumping ground.

| Principle | Decision |
|-----------|----------|
| **What we build** | Structured longitudinal health memory — trends, family compare, SQL/DuckDB analytics, access control |
| **What ChatGPT already does** | Read a PDF once, answer ad-hoc questions — **not our moat** |
| **Claude's role** | **Last resort at ingest only** — never first. After local PDF/OCR + rules fail |
| **Extraction order** | 1) PDF text → 2) Tesseract OCR → 3) Claude vision (opt-in) → 4) rules → 5) Claude text parse |
| **Staged pipeline** | Phase 1: local extract → save `extracted_text` in H2 → purge PDF. Phase 2: rules. Phase 3: Claude from DB text only if needed |
| **Auto-parse default** | `PHI_CLAUDE_AUTO_PARSE=true` for normal UX; `false` in dev to save tokens |
| **Vision fallback** | `PHI_CLAUDE_VISION_FALLBACK=false` by default — only when local text extraction fails completely |
| **Future chatbot** | Tool-calling over `biomarker_values` / DuckDB — **not** full PDF or history dumps per message |
| **Rules-first** | Regex/templates for known labs before Claude; Claude fills gaps only |

### Anti-patterns (never ship)

- Send full report history to Claude on every user question
- Store PDFs as long-term archive
- Re-parse on duplicate upload
- Build features that SQL can answer without an LLM

See [00-engineering-principles.md](./00-engineering-principles.md).

---

## Data sovereignty (structured DB is source of truth)

**Recorded 2026-09-18** — PDFs and images are **inputs**, not the system of record.

| Principle | Decision |
|-----------|----------|
| **Source of truth** | **H2 `biomarker_values` + related tables** (and DuckDB for analytics). All product features read structured data. |
| **PDF / image role** | **Ephemeral ingest artifact** — used only to extract text → biomarkers. Not relied on for display, trends, or future chatbot. |
| **Long-term PDF storage** | **Optional / discouraged.** Default path: delete file after successful extraction (+ short retention window for retry). |
| **Duplicate uploads** | Detect via **content hash** (e.g. SHA-256) per person/family — reject or link to existing report instead of re-inserting biomarkers. |
| **Retry extraction** | Prefer re-running on **stored `extracted_text`** in H2 if file already deleted; keep file only while status is `PENDING` / `PROCESSING` / `FAILED` (configurable TTL). |
| **Future chatbot** | Query **tabular biomarker + timeline data** (H2/DuckDB/API), not raw PDFs. |

### Why

- PDFs and images are hard to process reliably (OCR, layout, cost).
- Tabular biomarker rows are easy to query, trend, compare, and expose to an LLM with structured context.
- Discarding originals reduces privacy surface and disk use.

### Implementation status (2026-09-18)

| Capability | Status |
|------------|--------|
| `content_hash` on upload + dedup per person | **Done** — `409 duplicate_report` with `existingReportId` |
| Delete file after local text extract | **Done** — default `PHI_RETAIN_FILES_DAYS=0` |
| Nullable `storage_path` after purge | **Done** — migration `V5__ephemeral_report_storage.sql` |
| Staged pipeline: text in DB before Claude | **Done** — `TEXT_EXTRACTED` status, `extractAndPersistLocalText()` |
| Retry / parse from `extracted_text` | **Done** — `POST .../retry`, `POST .../parse` |
| `PHI_CLAUDE_AUTO_PARSE` config | **Done** — manual token control in dev |
| Rules-first extraction (Claude for gaps only) | **Done** — Orange Health; `PHI_CLAUDE_MIN_COVERAGE=0.8` |
| SQL-grounded chatbot | **Planned** |

---

## Medications & prescriptions

**Recorded 2026-09-19** — chronic-care context is a core reason families use PHI.

| Decision | Choice |
|----------|--------|
| **Primary input** | **Upload prescription photo/PDF** — extract name, dosage, schedule, start, duration |
| **Manual entry** | Fallback only (Settings) — not the main UX |
| **Course types** | **ACUTE** (malaria, dengue, short antibiotics) vs **CHRONIC** (diabetes, BP, thyroid) |
| **Chronic meds** | No automatic end date — `ended_on` null until user/doctor marks stopped |
| **Acute meds** | `duration_days` + `expected_end_on` from prescription |
| **AI role** | Parse unstructured prescription **text once at ingest** — same boundary as lab reports |
| **Product role** | **Log + correlate** with biomarker trends — **never** suggest dose changes |
| **Target users** | Older adults on lifelong medications + family maintainers uploading for them |

See [22-medication-and-prescriptions.md](./22-medication-and-prescriptions.md).

---

## Imaging reports (USG, X-ray, MRI, CT)

**Recorded 2026-09-19** — imaging context is as important as labs for family understanding.

| Decision | Choice |
|----------|--------|
| **Primary input** | Radiology **report PDF/photo** — impression + findings text |
| **Not in scope** | DICOM volumes, in-app scan viewer, pixel-level AI |
| **Structured storage** | `imaging_studies` + `imaging_findings` — source of truth |
| **Cross-modal value** | Correlate with labs (e.g. fatty liver USG + ALT) and meds |
| **AI role** | Parse report text once at ingest if heuristics fail |
| **User step** | Confirm extracted study before saving (like prescriptions) |

See [23-imaging-reports.md](./23-imaging-reports.md).

---

## Deferred (not decided yet)

- Phone OTP / social login timeline
- Exact maintainer vs admin permissions matrix
- Family switcher UI when person is in 2+ families
- Delete-report permissions (uploader only vs admin too)
- Phase 3b: rules-only vs AI-only for “what to do”
- Hindi / regional language
- Medical disclaimer copy

---

## Related docs

- [00-engineering-principles.md](./00-engineering-principles.md) — north star (read first)
- [07-extraction-pipeline.md](./07-extraction-pipeline.md) — staged ingest implementation
- [16-product-vision-family-and-modes.md](./16-product-vision-family-and-modes.md) — roles + multi-family
- [18-family-api-and-schema-plan.md](./18-family-api-and-schema-plan.md) — schema
