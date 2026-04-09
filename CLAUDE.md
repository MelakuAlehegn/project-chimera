# CLAUDE.md -- Project Chimera Agent Rules

## Project Context

This is **Project Chimera**, an autonomous influencer system. AI agents discover trends, generate content, verify quality, and publish to social media platforms.

- **Architecture**: Human -> Manager Agents -> Worker Agents -> Skills -> MCP Tools
- **Core module**: `chimera/chimera-core` (Maven, Java 21)
- **Current phase**: TDD "Red" -- tests exist and intentionally fail; implementation is the next step

## The Prime Directive

**NEVER generate code without checking the `specs/` directory first.**

- Before writing any implementation, review `specs/technical.md`, `specs/functional.md`, and `specs/_meta.md`.
- If a request conflicts with specs, the specs take precedence. Call out the conflict explicitly.
- If code diverges from specs, highlight the discrepancy and align new changes with specs.

## Java-Specific Directives

- **Java 21+**: Use records, pattern matching, sealed types, switch expressions, virtual threads.
- **Records for DTOs**: All data transfer objects passed between agents must be Java `record` types.
- **JUnit 5 for all tests**: Use `org.junit.jupiter.api`, clear naming, focused assertions.
- **Clean architecture**: Separate domain, application, and infrastructure concerns. Use interfaces and dependency inversion.

## Traceability

Before writing code:
1. **Explain your plan** -- what will change, which layers are involved, how it fits the agent workflow.
2. **Reference specs** -- cite the specific `specs/` files and sections driving the implementation.
3. **State assumptions** -- enumerate API, contract, and boundary assumptions explicitly.

## Build Commands

```bash
make setup       # mvn clean install -DskipTests
make test        # mvn test (expect failures in Red phase)
make lint        # mvn checkstyle:check
make build       # mvn package -DskipTests
make spec-check  # verify code aligns with specs/
make docker-test # build and run tests inside Docker
```
