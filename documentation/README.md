# PHI Technical Documentation

Detailed reference for the Personal Health Intelligence system.

## Start here

| Doc | What it covers |
|-----|----------------|
| **[00-engineering-principles.md](./00-engineering-principles.md)** | **North star — read first.** DB as truth, Claude used sparingly, not a chat wrapper |
| [01-project-overview.md](./01-project-overview.md) | Goals, stack, repo layout, phases |
| [02-system-architecture.md](./02-system-architecture.md) | Components, diagrams, design decisions |
| [03-data-flow.md](./03-data-flow.md) | Staged upload → local extract → parse → poll |

## Backend

| Doc | What it covers |
|-----|----------------|
| [04-api-reference.md](./04-api-reference.md) | REST endpoints, request/response shapes |
| [05-database-schema.md](./05-database-schema.md) | Tables, migrations, JPA entities |
| [06-backend-services.md](./06-backend-services.md) | Services, config beans, package map |
| [07-extraction-pipeline.md](./07-extraction-pipeline.md) | Staged pipeline: local text → DB → optional Claude |

## Client & ops

| Doc | What it covers |
|-----|----------------|
| [08-mobile-application.md](./08-mobile-application.md) | Expo app, API client, upload flow |
| [09-configuration.md](./09-configuration.md) | Env vars, YAML profiles, secrets |
| [10-security.md](./10-security.md) | Auth, CORS, current gaps |
| [11-infrastructure-deployment.md](./11-infrastructure-deployment.md) | Edge server, Caddy, backup, deploy |

## Product & platform

| Doc | What it covers |
|-----|----------------|
| [16-product-vision-family-and-modes.md](./16-product-vision-family-and-modes.md) | One app, family peers, basic vs advanced mode |
| [17-hybrid-data-architecture.md](./17-hybrid-data-architecture.md) | H2 (OLTP) + DuckDB (analytics dashboards) |
| [18-family-api-and-schema-plan.md](./18-family-api-and-schema-plan.md) | Auth, invites, schema, API blueprint |
| [19-product-decisions.md](./19-product-decisions.md) | Locked product Q&A (auth, data sovereignty, AI usage) |
| [20-village-and-scanned-reports.md](./20-village-and-scanned-reports.md) | Scans, photos, village labs — primary users |
| [21-ops-monitoring.md](./21-ops-monitoring.md) | Pipeline telemetry, tokens, ops APIs |

## Domain & quality

| Doc | What it covers |
|-----|----------------|
| [12-rules-skills-golden-data.md](./12-rules-skills-golden-data.md) | Rules YAML, skills markdown, test dataset |
| [13-testing.md](./13-testing.md) | Unit tests, gaps, golden regression plan |
| [14-local-development.md](./14-local-development.md) | Step-by-step dev setup |
| [15-roadmap-and-gaps.md](./15-roadmap-and-gaps.md) | Built vs planned, known inconsistencies |

## Related folders

| Path | Purpose |
|------|---------|
| [../sprints/](../sprints/) | Feature tracking (current, backlog, completed) |
| [../docs/raw-data-on-this-project.md](../docs/raw-data-on-this-project.md) | Clinical domain notes and test case source |
| [../README.md](../README.md) | Quick-start guide |

_Last updated: 2026-09-18_
