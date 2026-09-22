# Sprint 05 — Engineered Intelligence

**Status:** planned (next)  
**North star:** [documentation/00-engineering-principles.md](../documentation/00-engineering-principles.md)

---

## Why this sprint exists

PHI is **not** a ChatGPT wrapper or PDF dumping ground. Generic chat can read a lab PDF once. Our moat is:

- Structured biomarker memory over years
- SQL/DuckDB trends and family compare
- Deterministic rules for clinical signals
- Claude **only** where rules cannot parse messy lab layouts

This sprint builds intelligence **on top of the database**, not on top of more API calls.

---

## Sub-sprints

| Sub-sprint | Focus | Claude? |
|------------|-------|---------|
| **5a** | Rules-first extraction | Only if coverage &lt; threshold |
| **5b** | Deterministic reasoning (`rules/*.yaml`) | **No** |
| **5c** | SQL-grounded insight cards | **No** |
| **5d** | Chatbot with SQL tools | Small context only — **not** PDF dumps |

Pull **5a** into [CURRENT.md](./CURRENT.md) when work starts.

---

## Sprint 5a — Rules-first extraction

### Goal

Parse known lab formats (starting with Orange Health) using templates/regex **before** calling Claude. Claude fills gaps only.

### Acceptance criteria

- [x] `RuleBasedExtractor` returns biomarkers + `coverage` (0.0–1.0)
- [x] Orange Health extractor passes golden dataset test **without API key** (~77 biomarkers, ≥80% coverage)
- [x] `ReportExtractionService` calls rules before Claude; skips Claude if coverage ≥ `PHI_CLAUDE_MIN_COVERAGE`
- [x] When Claude runs, it uses full `extracted_text` from H2 as fallback (gap-fill path for future sprint)
- [x] Dedup + `COMPLETED` guard unchanged — no double spend
- [x] Docs updated in `07-extraction-pipeline.md`

### Out of scope

- Chat UI
- Explaining biomarkers to users via LLM
- Re-sending PDFs to Claude

---

## Sprint 5b — Deterministic reasoning

### Goal

Evaluate `biomarker_values` against `rules/*.yaml` — severity, recommended actions — **zero Claude cost**.

### Acceptance criteria

- [ ] `ReasoningModule` loads YAML rules
- [ ] `GET /api/v1/reports/{id}/findings` returns structured findings
- [ ] Mobile report detail shows out-of-range from findings API
- [ ] Basic mode home shows action cards from findings (template text)

---

## Sprint 5c — SQL-grounded insights

### Goal

Surface “what changed” and family snapshots from DuckDB — no LLM.

### Acceptance criteria

- [ ] Delta API: biomarker change vs previous report per person
- [ ] Home screen insight cards from SQL aggregates
- [ ] Family abnormal count widget (advanced mode)

---

## Sprint 5d — Chatbot foundation (later)

### Goal

Grounded Q&A via **SQL tools**, not document dumps.

### Anti-patterns (do not ship)

- Paste full PDF into prompt
- Send entire biomarker history per message
- “Ask anything about your health” without structured retrieval

### Correct pattern

```
User: "Is my Vitamin D improving?"
  → tool: get_biomarker_timeline(person_id, "vitamin_d")
  → returns 5–10 JSON rows
  → optional short NL summary from small context
```

---

## Verification

| Check | Command / action |
|-------|------------------|
| Rules tests (no Claude) | `./gradlew test --tests '*Rule*'` |
| Golden regression | `./gradlew test --tests com.phi.golden.*` |
| Full suite | `./gradlew test` |
| Manual upload | Upload golden PDF → verify biomarker count without Claude when rules suffice |

---

## Retro questions (end of sprint)

1. What % of Orange Health reports parsed without Claude?
2. Did we add any feature that ChatGPT already does equally well?
3. Are all new read paths free of Claude calls?
