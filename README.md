# Project Chimera

**An autonomous influencer network where AI agents create and distribute social media content.**

---

## Project Overview

Project Chimera is a **Java 21 AI Swarm Orchestrator**. It coordinates Manager and Worker agents that discover trends, generate content, verify quality, and publish to platforms. The architecture follows:

**Human → Manager Agents → Worker Agents → Skills → MCP Tools**

The codebase is built with **Maven** and **Java 21**, using **Records** for immutable data and **JUnit 5** for tests. Specs in `specs/` define contracts; skills in `skills/` define reusable capabilities; and the core module `chimera/chimera-core` holds the implementation surface that agents and CI/CD use.

---

## Architecture Status: Red (TDD) Phase

The project is currently in the **"Red" phase** of Test-Driven Development:

- **Tests exist and fail by design.** JUnit 5 tests in `chimera/chimera-core/src/test/java/com/chimera/` define the implementation contract (e.g. `TrendFetcherTest`, `ContentGeneratorTest`).
- **Implementation "slots" are filled with minimal types** (records, interfaces, exceptions) so the project **compiles**; the tests **fail** because behavior is not yet implemented.
- This is intentional: the failing tests are the **orchestrator’s goal posts**. Any human or AI agent pushing code must satisfy these tests before the build turns green. Until then, **a red test run is expected** and indicates that the contract is enforced and the next step is to implement the behavior under test.

---

## Automation

All commands run against `chimera/chimera-core` via the root **Makefile**. Use these from your terminal or from CI/CD:

| Command       | Purpose |
|--------------|--------|
| `make setup` | Resolve dependencies: `mvn clean install -DskipTests` for chimera-core. |
| `make test`  | Run the JUnit 5 test suite. In the Red phase, tests fail by design. |
| `make lint`  | Run code quality checks (e.g. `mvn checkstyle:check`) for Git hygiene. |
| `make build` | Build the JAR: `mvn package -DskipTests`. |
| `make help`  | List available Make targets. |

**Quick start:**

```bash
make setup   # install dependencies
make test    # run tests (expect failures in Red phase)
make build   # produce JAR without running tests
```

---

## Governance Layer

Every change is guarded by:

1. **GitHub Actions CI** (`.github/workflows/main.yml`)
   - Runs on **push** and **pull_request** to `main`.
   - Uses **Java 21 (Temurin)** on `ubuntu-latest`.
   - Executes `make setup` and `make test`. The pipeline fails if tests fail, enforcing the TDD contract.

2. **CodeRabbit AI Review** (`.coderabbit.yaml`)
   - The **Chimera Guardian** AI reviewer enforces:
     - **Spec alignment**: Code must match `specs/technical.md` and related specs (API contracts, schemas).
     - **Java thread safety**: Prefer Records, final fields, avoid shared mutable state.
     - **Security**: No hardcoded credentials or insecure API patterns.
     - **Virtual Threads**: Prefer Virtual Threads and `StructuredTaskScope` where appropriate instead of blocking platform threads.

Together, CI and AI review provide a **black-box audit trail**: every line of code is tested by CI and reviewed against the Chimera policy before merge.

---

## Tech Stack

- **Java 21** — Records for immutable DTOs, Virtual Threads and Structured Concurrency where applicable.
- **Maven** — Build and dependency management; core module: `chimera/chimera-core`.
- **JUnit 5** — All tests; defines the implementation contract in the Red phase.
- **Specs** — `specs/technical.md`, `specs/openclaw_integration.md`, etc., define APIs and architecture.
- **Skills** — Reusable capabilities (e.g. TrendFetcher, ContentGenerator) under `skills/`.

---

## Why the Build "Fails"

If you run `make test` and see **failures**, that is expected in the current phase:

- The tests describe *what* the system should do; the implementations are minimal stubs.
- **Red** means the contract is in place; **Green** will come when behavior is implemented to satisfy the tests.
- This keeps the bar clear for both humans and AI agents: **pass the tests and pass the AI review** to merge.

For a green build, implement the behavior required by the failing tests (and satisfy lint and CodeRabbit). Until then, the failing tests are the orchestrator’s goal posts.
