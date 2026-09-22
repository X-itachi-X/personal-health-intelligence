# Project Overview

## Mission

**Personal Health Intelligence (PHI)** turns uploaded lab reports into **structured, longitudinal health data** for families — trends, comparisons, and clear summaries grounded in a real database.

The product goal is not to replace a doctor. It is to help a family understand lab results, spot patterns over time, and know what to discuss at the next visit.

**PHI is not a ChatGPT wrapper.** Generic chat can read a PDF once. PHI engineers persistent structured memory, SQL/DuckDB analytics, family context, and careful AI only where rules cannot parse messy lab layouts. See [00-engineering-principles.md](./00-engineering-principles.md).

## Target users

- Primary: family members uploading their own lab PDFs
- Scale: single household to many families (multi-tenant on one edge server, privacy-first)

## Technology stack

| Layer | Technology | Version / notes |
|-------|------------|-----------------|
| Mobile | Expo (React Native) + TypeScript | SDK 57 |
| Backend | Spring Boot + Java | Boot 4, Java 21+ |
| OLTP database | H2 embedded file DB | **Source of truth** — biomarkers, reports, auth |
| Analytics | DuckDB embedded | Trends, compare — synced from H2 |
| PDF parsing | Apache PDFBox | 3.0.4, local only (Phase 1 ingest) |
| AI parsing | Anthropic Claude API | **Ingest boundary only** — text → JSON, once per report |
| Reverse proxy (prod) | Caddy | Port 80 → 8080 |
| Host OS (prod) | Alpine Linux on Proxmox VM | 4 GiB RAM |

## Repository layout

```
personal-health-intelligence/
├── mobile/              Expo app (upload, poll, trends, family)
├── backend/             Spring Boot API + staged extraction engine
├── rules/               Deterministic health rules (YAML) — planned for reasoning layer
├── skills/              Medical domain knowledge (markdown) — not wired to Claude dumps
├── data/golden_dataset/ Expected outputs for regression tests
├── infrastructure/edge/ Deploy scripts for home server
├── documentation/       Technical docs — start at 00-engineering-principles.md
├── sprints/             Feature tracking
└── docs/                Long-form domain notes
```

## Development phases

| Phase | Focus | Status |
|-------|-------|--------|
| 1. Setup | Scaffold, infra, golden dataset layout | Done |
| 2. Core engineering | Staged PDF extraction, biomarker parsing | Done |
| 3a. Family platform | Auth, families, invites, per-person upload | Done |
| 3b. Modes + intelligence | Basic/advanced UI, rules-first reasoning | In progress |
| 4. Analytics | DuckDB hybrid, dashboards, golden tests | Done |
| 5. Polish | Reminders, edge deploy, web dashboard | Planned |

**Product vision:** [16-product-vision-family-and-modes.md](./16-product-vision-family-and-modes.md)  
**Engineering north star:** [00-engineering-principles.md](./00-engineering-principles.md)

See [15-roadmap-and-gaps.md](./15-roadmap-and-gaps.md) and [../sprints/](../sprints/) for current status.

## Key design decisions

### Database is the source of truth

All product features — UI, trends, family compare, future chatbot — read **structured rows** in H2 (and DuckDB for analytics). PDFs and images are **ephemeral ingest artifacts**, not the system of record.

### Staged ingest (not dump-to-Claude)

1. **Phase 1 (local, free):** PDFBox extracts text → save `extracted_text` in H2 → dedup by content hash → delete PDF
2. **Phase 2 (Claude, costs tokens):** Read text from DB → biomarker JSON → `biomarker_values` → `COMPLETED`

Claude is never the default brain of the app. See [07-extraction-pipeline.md](./07-extraction-pipeline.md).

### H2 + DuckDB hybrid

H2 handles OLTP (uploads, auth, biomarkers). DuckDB handles analytics queries (trends, family compare). See [17-hybrid-data-architecture.md](./17-hybrid-data-architecture.md).

### Async extraction with polling

Upload returns immediately (`PENDING`). Background workers run Phase 1 then optional Phase 2. Mobile polls until `COMPLETED`, `TEXT_ONLY`, or `FAILED`.

### Family platform

JWT auth, multi-family membership, per-person upload with ACL, audit log. Upload for another person is auto-visible to that person.

## What works today

1. Upload lab PDF from phone (with duplicate detection)
2. Local text extraction saved to H2; PDF purged by default
3. Optional Claude parse → structured biomarkers in H2
4. DuckDB sync for trends and family analytics
5. Mobile app: upload, poll, reports, trends, family, settings
6. Golden dataset regression tests (106+ biomarkers from real Orange Health PDF)

## What we are building next (engineered, not chat dumps)

- **Rules-first extraction** — known lab templates via regex; Claude only for gaps
- **Deterministic reasoning** — out-of-range, severity from `rules/*.yaml`
- **SQL-grounded insights** — trends and comparisons without re-sending PDFs
- **Future chatbot** — tool-calling over `biomarker_values`, not document dumps

## What PHI deliberately avoids

- Storing PDFs as long-term archive
- Sending full report history to Claude on every user question
- Features that ChatGPT/Gemini already do equally well without structured memory
- Re-parsing the same report on duplicate upload
