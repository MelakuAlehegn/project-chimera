# Project Chimera -- Meta Specification

## Vision

Build **Autonomous AI Influencers** -- digital entities that research trends, generate social media content, and manage engagement without human intervention. The system is a Java 21 AI Swarm Orchestrator where Manager agents plan, Worker agents execute, Judge agents verify, and Skills provide concrete capabilities exposed via MCP tools.

## Architecture Overview

```
Human (Safety Layer)
  |
  v
Manager Agents  -- plan campaigns, allocate budgets
  |
  v
Worker Agents   -- execute skills (fetch trends, generate content, publish)
  |
  v
Judge Agents    -- verify quality, safety, brand alignment
  |
  v
Skills Layer    -- reusable capabilities (TrendFetcher, ContentGenerator, ContentVerifier, PlatformPublisher)
  |
  v
MCP Tool Layer  -- external API bridges (social platforms, LLMs, databases)
```

## Constraints

| Constraint | Detail |
|---|---|
| **Language & Runtime** | Java 21+ (records, sealed types, pattern matching, virtual threads) |
| **Spec-Driven Development (SDD)** | No implementation code is written until the specification is ratified. Specs in `specs/` are the single source of truth. |
| **MCP Traceability** | Every agent action must be traceable through MCP tool invocations. The Tenx MCP Sense server records the "black box" flight log. |
| **Immutability** | All DTOs passed between agents use Java `record` types -- no mutable state crosses agent boundaries. |
| **Testing** | JUnit 5 for all tests. Tests are written before implementation (TDD). Failing tests define the contract. |
| **Build Tool** | Maven. Core module: `chimera/chimera-core`. |
| **CI/CD** | GitHub Actions runs `make test` on every push. CodeRabbit AI enforces spec alignment, thread safety, and security. |
| **Concurrency** | Prefer virtual threads (`Thread.ofVirtual()`) and `StructuredTaskScope` over platform thread pools for agent orchestration. |

## Non-Goals

The following are explicitly **out of scope** for the current phase:

- **Actual social media API integration** -- skills define contracts only; real API calls are deferred.
- **Frontend / UI** -- no web dashboard or React frontend in this phase.
- **Model training** -- the system orchestrates LLMs via MCP tools; it does not train or fine-tune models.
- **Multi-language support** -- Java 21 only; no polyglot runtimes.
- **Production deployment** -- no Docker, Kubernetes, or cloud infra provisioning in this phase (bonus: Dockerfile for local testing).
- **Billing / payment processing** -- the Resource Governor tracks budgets but does not process payments.

## Key Decisions

1. **Agent Pattern**: Hierarchical Swarm -- Manager agents decompose goals, Workers execute, Judges verify. Not a flat sequential chain.
2. **Human-in-the-Loop**: The Safety Layer allows human approval before publishing. Humans set goals and budgets; agents execute autonomously within those bounds.
3. **Database**: Relational (PostgreSQL target) for structured trend and video metadata. Schema defined in `specs/technical.md`.
4. **OpenClaw Integration**: Agents publish availability and capabilities to the OpenClaw agent social network (see `specs/openclaw_integration.md`).
