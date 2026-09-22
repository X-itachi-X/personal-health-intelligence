# Rules, Skills & Golden Dataset

Domain knowledge and test data for the reasoning layer (Phase 3).

**Status:** Files exist. **No runtime integration yet.**

---

## Rules (`rules/`)

Deterministic YAML rules that evaluate biomarker values.

### Example: `rules/cardiovascular.yaml`

```yaml
id: elevated_lp_a
domain: cardiovascular
inputs:
  - lp_a
rules:
  - when: "lp_a > 30"
    severity: high
    action: doctor_discussion
    message: "Lp(a) is genetically influenced; discuss long-term cardiovascular risk with your doctor."
```

### Planned rule structure

| Field | Purpose |
|-------|---------|
| `id` | Unique rule identifier |
| `domain` | Medical category |
| `inputs` | Canonical biomarker IDs required |
| `rules[].when` | Expression evaluated against values |
| `rules[].severity` | `low`, `medium`, `high` |
| `rules[].action` | Suggested action type |
| `rules[].message` | User-facing text |

### Planned integration

```
biomarker_values → RuleEngine → List<Finding>
```

`ReasoningModule` (`com.phi.reasoning`) is a placeholder — no YAML loader exists.

---

## Skills (`skills/`)

Markdown files with medical domain context for AI explanations.

### Example: `skills/hematology.md`

Contains notes such as:

- CBC interpretation patterns for the patient
- Iron supplementation cautions
- When to follow up with Hb electrophoresis

### Planned usage

Skills provide **context** to Claude when generating explanations — not deterministic logic.

```
findings[] + skills/hematology.md → Claude → plain-language summary
```

**Not referenced by any Java code today.**

---

## Golden dataset (`data/golden_dataset/`)

Expected extraction and reasoning outputs for regression testing.

### Structure

```
data/golden_dataset/
├── README.md
└── ankit/
    ├── report_2026_09.json          # 106 biomarkers (Orange Health full panel)
    └── ankit_6972960_..._merged.pdf # Source PDF
```

### `report_2026_09.json` format

```json
{
  "patient": "ankit",
  "report_date": "2026-09-06",
  "source": "docs/raw-data-on-this-project.md",
  "biomarkers": [
    {
      "canonical": "lp_a",
      "value": 85.2,
      "unit": "mg/dL",
      "reference": "< 30"
    }
  ],
  "expected_findings": [
    "elevated_lp_a",
    "vitamin_d_insufficient",
    "microcytic_rbc_pattern"
  ]
}
```

| Field | Purpose |
|-------|---------|
| `biomarkers[]` | Ground-truth values for key tests |
| `expected_findings[]` | Rule IDs that should fire |

**106 biomarkers** extracted from the Orange Health PDF via Claude (`GoldenDatasetGeneratorTest`). Original 12-value subset came from `docs/raw-data-on-this-project.md`.

### Test harness (implemented)

- `GoldenDatasetRegressionTest` — validates fixture JSON structure
- `ReportExtractionGoldenTest` — persists golden biomarkers and asserts values match
- `GoldenDatasetGeneratorTest` — manual regeneration (`GENERATE_GOLDEN=true`)

```bash
cd backend && ./gradlew test --tests 'com.phi.golden.*'
```

---

## Relationship to extraction

| Layer | Input | Output | Status |
|-------|-------|--------|--------|
| Extraction | PDF text | `biomarker_values` | ✅ Built |
| Rules | `biomarker_values` | `findings[]` | ⬜ Planned |
| Skills + AI | `findings[]` + skills | `summary` text | ⬜ Planned |

---

## Adding new rules

1. Create `rules/{domain}.yaml`
2. Use canonical biomarker IDs from `BiomarkerNormalizer` (e.g. `lp_a`, `vitamin_d`)
3. When rule engine is built, rules will be loaded at startup or on demand

## Adding golden test cases

1. Add JSON under `data/golden_dataset/{person}/`
2. Include `biomarkers` and `expected_findings`
3. Wire into JUnit test when regression harness exists

---

## Related documentation

- Clinical source data: [../docs/raw-data-on-this-project.md](../docs/raw-data-on-this-project.md)
- Sprint notes: [../sprints/sprint-01-phase2-extraction.md](../sprints/sprint-01-phase2-extraction.md)
