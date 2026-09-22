# Sprints

Track what we're building, what's done, and what's next.

**North star:** [documentation/00-engineering-principles.md](../documentation/00-engineering-principles.md) — engineered health data platform, not a chat wrapper.

## Phase map

| Phase | Focus | Status |
|-------|-------|--------|
| 1 | Scaffold | ✅ Done |
| 2 | Staged extraction + biomarker persistence | ✅ Done |
| 3a | Family platform (auth, invites, ACL) | ✅ Done |
| 3b | Basic vs advanced UX | 🔶 Partial |
| 4 | Analytics (H2 + DuckDB) | ✅ Done |
| 4b | Data sovereignty (ephemeral PDFs, dedup, staged Claude) | ✅ Done |
| **5** | **Engineered intelligence** (rules-first, SQL insights) | **⬜ Next** |
| 6 | Polish & deploy | ⬜ Planned |

## Files

| File | Purpose |
|------|---------|
| [CURRENT.md](./CURRENT.md) | Active sprint — features in progress right now |
| [BACKLOG.md](./BACKLOG.md) | Planned features by phase |
| [COMPLETED.md](./COMPLETED.md) | Shipped features — newest at top |
| [sprint-05-engineered-intelligence.md](./sprint-05-engineered-intelligence.md) | Phase 5 goals, acceptance criteria, anti-patterns |

## Status labels

- `done` — shipped and verified
- `in-progress` — actively being built
- `blocked` — waiting on something (note why)
- `planned` — agreed but not started
- `partial` — started, not complete

## Workflow

1. Add ideas to **BACKLOG.md** — check they align with [00-engineering-principles.md](../documentation/00-engineering-principles.md)
2. Move items to **CURRENT.md** when a sprint starts
3. When finished, move to **COMPLETED.md** with date
4. For multi-week work, use `sprint-NN-*.md`

## Before adding AI features — ask

1. Can SQL/DuckDB answer this without Claude?
2. Is this ingest-boundary only (once per report)?
3. Would ChatGPT already do this equally well?

If yes to #3, we're not adding value.

## Technical reference

[documentation/README.md](../documentation/README.md)
