# Project Chimera

*An autonomous influencer network where AI agents discover trends, generate content, verify quality, and publish to social media platforms -- all orchestrated via a Java 21 AI Swarm with MCP tool traceability.*

---

## Quick Start

```bash
make setup       # Resolve dependencies (mvn clean install -DskipTests)
make test        # Run JUnit 5 tests (expect failures -- TDD Red phase)
make lint        # Run Checkstyle code quality checks
make build       # Build JAR (mvn package -DskipTests)
make spec-check  # Verify code aligns with specs/
make docker-test # Build and run tests inside Docker
```

Requires: **Java 21+** and **Maven 3.8+** (Docker optional for `make docker-test`)

---

## Architecture

```
Human (Safety Layer)
  -> Manager Agents (plan, coordinate)
    -> Worker Agents (execute skills)
      -> Judge Agents (verify quality)
        -> Skills (TrendFetcher, ContentGenerator, ContentVerifier, PlatformPublisher)
          -> MCP Tools (external API bridges)
```

---

## Why Tests Fail (Intentional)

This repo is in the **TDD "Red" phase**. Tests define the implementation contract but the behavior is not yet implemented:

- `TrendFetcherTest` -- asserts the TrendFetcher API contract (records, input/output shapes). Fails because `fetchTrends()` throws `UnsupportedOperationException`.
- `SkillsInterfaceTest` -- asserts that skill interfaces (ContentGenerator) accept correct parameters and declare `BudgetExceededException`. All reflective assertions pass; this validates the type contracts are in place.
- `ContentGeneratorTest` -- same skill interface tests (original file retained alongside SkillsInterfaceTest).

The failing tests are the **goal posts**: implement the behavior to turn them green.

---

## Repository Structure

```
project_chimera/
|-- specs/                          # Specifications (source of truth)
|   |-- _meta.md                    # Vision, constraints, non-goals
|   |-- functional.md               # User stories (trends, content, publishing, governance)
|   |-- technical.md                # API contracts (JSON shapes), database ERD
|   |-- openclaw_integration.md     # OpenClaw agent network protocol (bonus)
|
|-- skills/                         # Agent skill contracts (4 skills)
|   |-- skill_trend_fetcher/        # Fetch trending topics
|   |-- skill_content_generator/    # Generate video scripts
|   |-- skill_content_verifier/     # Verify content safety/quality
|   |-- skill_platform_publisher/   # Publish to platforms
|
|-- chimera/chimera-core/           # Maven module (Java 21, JUnit 5)
|   |-- pom.xml                     # Build config (Java 21, JUnit 5, Checkstyle)
|   |-- src/main/java/com/chimera/  # Records, interfaces, exceptions (stubs)
|   |-- src/test/java/com/chimera/  # JUnit 5 test sources
|
|-- .cursor/rules/                  # AI agent rules
|   |-- chimera_rules.mdc           # Prime Directive, Java directives, traceability
|
|-- scripts/spec-check.sh            # Spec-vs-code alignment checker
|-- Dockerfile                       # Containerized test runner
|-- CLAUDE.md                        # Claude Code agent rules (equivalent to .cursor/rules)
|-- .coderabbit.yaml                 # AI review policy (spec alignment, thread safety, security)
|-- .github/workflows/main.yml       # CI: make setup -> make lint -> make test
|-- Makefile                         # setup, test, lint, build, spec-check, docker-test
```

---

## Governance

1. **GitHub Actions CI** -- runs `make setup`, `make lint`, and `make test` on every push and PR to `main`. Uses Java 21 (Temurin).
2. **CodeRabbit AI Review** -- enforces spec alignment, Java thread safety, security vulnerability checks, and virtual thread best practices.
3. **Agent Rules** -- `.cursor/rules/chimera_rules.mdc` and `CLAUDE.md` ensure AI agents check specs before generating code.

---

## Tech Stack

| Component | Choice |
|---|---|
| Language | Java 21 (records, virtual threads, pattern matching) |
| Build | Maven |
| Tests | JUnit 5 |
| Lint | Checkstyle (via maven-checkstyle-plugin) |
| CI | GitHub Actions |
| AI Review | CodeRabbit |
| Data Transfer | Immutable Java records |
