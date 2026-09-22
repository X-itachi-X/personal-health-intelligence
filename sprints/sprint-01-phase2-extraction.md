# Sprint 01 — Phase 2 extraction

**Dates:** 2026-09-07  
**Goal:** Upload a lab PDF from the phone and get structured biomarkers back.

## Planned

- [x] PDF upload endpoint
- [x] PDFBox text extraction
- [x] Claude API client + JSON parser
- [x] Async extraction pipeline
- [x] Mobile upload + status polling
- [x] H2 schema for extraction status + biomarkers

## Shipped

End-to-end flow works: mobile upload → backend stores PDF → Claude parses text → 131 biomarkers saved for full body checkup PDF.

## Issues hit (for reference)

| Issue | Fix |
|-------|-----|
| FormData broken on Expo Android | XHR upload path in `mobile/lib/api.ts` |
| Missing `anthropic-workspace-id` | `CLAUDE_WORKSPACE_ID` in `.env` |
| Retired model `claude-sonnet-4-20250514` | Switched to `claude-sonnet-4-6` |
| Response truncated at 4096 tokens | Raised to 16384 |
| Async `@Transactional` self-invocation | Split prepare / complete extraction methods |
| Mobile poll timeout (60s) | Extended to 90 attempts × 2s |

## Retro

- Extraction takes ~2–3 minutes for large panels — UI must set expectations.
- Golden dataset tests not yet wired — good next step before reasoning engine.
