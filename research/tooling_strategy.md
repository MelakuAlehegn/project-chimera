## Tooling Strategy for Project Chimera

This document describes the MCP-based tooling strategy for Project Chimera, an autonomous influencer system built with agent swarms. It focuses strictly on:
- Developer MCP tools used during development
- Runtime MCP tools used by agents in production
- The IDE + AI-assisted development environment

---

## 1. Developer MCP Tools (USED DURING DEVELOPMENT)

Developer MCP tools are invoked from the AI assistant inside Cursor while humans are building and evolving Chimera. They provide safe access to telemetry, version control, the filesystem, and specs.

### tenxfeedbackanalytics (Tenx MCP feedback logging)

- **Purpose**
  - Configured in `.cursor/mcp.json` as the `tenxfeedbackanalytics` MCP server (`tenxanalysismcp`).
  - Logs passage-of-time and performance-related events about development sessions (e.g., `log_passage_time_trigger`).
  - Provides meta-observability over how agents and humans collaborate during the Chimera build.
- **Why it is necessary for AI-assisted development**
  - Enables continuous improvement of AI-assisted workflows by capturing task patterns, complexity, and interaction quality.
  - Helps detect stalls or inefficiencies in agent-assisted development, informing rule or process changes without touching production agents.
- **Example usage during the Chimera build process**
  - While designing the `TrendFetcher` integration defined in `specs/technical.md`, the assistant logs a passage-time snapshot describing:
    - The current goal (e.g., “implement TrendFetcher persistence into the `Trends` table”).
    - How many turns it took to clarify requirements.
    - Observed competencies and bottlenecks during implementation.

### git-mcp (version control operations through MCP)

- **Purpose**
  - Exposes Git operations (status, diff, branch, commit, etc.) as MCP tools.
  - Allows the assistant to reason about the current working tree and guide clean, incremental commits without running raw shell commands directly from user prompts.
- **Why it is necessary for AI-assisted development**
  - Gives the AI visibility into what has changed, what is staged, and how large a change set is before suggesting further edits.
  - Encourages small, reviewable commits that match Chimera’s clean architecture approach and make it easy to trace changes back to specs.
- **Example usage during the Chimera build process**
  - After adding a new Java record DTO for a Planner → Worker message, plus associated JUnit 5 tests, the assistant:
    - Uses `git-mcp` to inspect `git status` and `git diff` for the `chimera-core` module.
    - Summarizes the changes in terms of spec alignment (e.g., mapping to `TrendFetcher` fields or OpenClaw capabilities).
    - Suggests a concise commit message tied to the relevant spec section.

### filesystem-mcp (reading/writing repository files)

- **Purpose**
  - Provides controlled read/write access to repository files through MCP, mirroring what tools like `Read` and patching do inside the IDE.
  - Enables the assistant to open, inspect, and edit files without direct shell access.
- **Why it is necessary for AI-assisted development**
  - Ensures all file operations are explicit, auditable, and scoped to the repository.
  - Allows the assistant to perform refactors, create new spec-aligned modules, or update tests while preserving project conventions.
- **Example usage during the Chimera build process**
  - When introducing a new Worker agent capability for `video_generation`:
    - The assistant uses `filesystem-mcp` to read existing Java modules and tests in `chimera-core`.
    - Writes a new record DTO plus corresponding JUnit 5 tests.
    - Updates any relevant configuration or documentation files, such as new capabilities reflected in OpenClaw messages.

### spec-reader capability (reading `specs/` before generating code)

- **Purpose**
  - Systematically reads and interprets documents in the `specs/` directory before any code is generated or modified.
  - Key inputs include:
    - `specs/technical.md` (TrendFetcher API, `Trends` and `Videos` schema)
    - `specs/openclaw_integration.md` (agent availability and capability announcements)
- **Why it is necessary for AI-assisted development**
  - Enforces the prime directive: **never generate code without checking the `specs/` directory first.**
  - Ensures that any new DTOs, services, or agent behaviors are traceable back to clear, text-based specifications.
  - Reduces drift between implementation and design, especially for cross-cutting concerns like trend analysis and publishing flows.
- **Example usage during the Chimera build process**
  - Before implementing a Manager → Worker coordination flow for `trend_analysis`:
    - The assistant uses the spec-reader capability to parse `specs/technical.md`, extracting the `TrendFetcher` input/output contract.
    - It then proposes Java 21+ record types that mirror the spec, along with repository methods aligned with the `Trends` and `Videos` tables.
    - Only after this spec pass does it generate code and tests.

---

## 2. Runtime MCP Tools (USED BY AGENTS IN PRODUCTION)

Runtime MCP tools are invoked by Chimera’s Manager and Worker agents while serving real influencer workloads. They are wired into the agent swarm via Skills and must have stable contracts.

The execution path is:
**Human → Manager Agents → Worker Agents → Skills → MCP Tools**

### Trend APIs (for trend discovery)

- **Role in architecture**
  - Expose endpoints like the `TrendFetcher` API defined in `specs/technical.md`, where agents supply parameters such as `platform` (`"tiktok"`) and `category` (`"fitness"`), and receive a list of trend objects (`topic`, `engagementScore`, etc.).
  - Worker agents with `trend_analysis` capability call these MCP tools through Skills to retrieve real-time trend signals.
- **Connection to the swarm**
  - Humans define goals (e.g., “find high-engagement fitness trends on TikTok”).
  - Manager agents decompose these into subtasks for Worker agents.
  - Worker agents invoke a Trend API Skill, which calls the underlying MCP tool and returns structured trends used for content planning.

### Content generation services

- **Role in architecture**
  - Given trend information and influencer personas, generate scripts, captions, and other assets for short-form content.
  - May be multiple MCP tools (e.g., “script-generator”, “caption-refiner”), each wrapped by a Skill.
- **Connection to the swarm**
  - Manager agents create content-generation subtasks from trend insights.
  - Worker agents specializing in `video_generation` use these Skills to:
    - Turn `TrendFetcher` output into draft scripts.
    - Format content for specific platforms and personas.

### Content verification/moderation tools

- **Role in architecture**
  - Check generated content for safety, brand alignment, and platform policy compliance.
  - Provide structured feedback (e.g., “approve”, “revise”, “reject with reasons”), allowing Judge or QA-style agents to act.
- **Connection to the swarm**
  - After Worker agents generate content, Manager or Judge agents call moderation Skills that wrap MCP tools.
  - Decisions from these tools feed back into the swarm:
    - Approved content is queued for publishing.
    - Rejected content triggers automatic revisions or escalations to humans.

### Social media publishing APIs

- **Role in architecture**
  - Publish finalized content (scripts, video references, captions) to external social media platforms.
  - Update internal state (e.g., `Videos.publish_status` defined in `specs/technical.md`) to reflect what has been posted.
- **Connection to the swarm**
  - Manager agents orchestrate publishing waves based on trend freshness and schedule.
  - Worker agents use Skills that wrap MCP tools for each platform’s API.
  - Successful publishes update database tables through complementary runtime tools, keeping Chimera’s view of the world in sync with external platforms.

---

## 3. IDE + AI Development Environment

The development environment for Project Chimera is centered on Cursor, with context engineering and MCP integration shaping how developers and AI collaborate.

### Cursor IDE

- Serves as the primary editing environment where humans and the AI assistant co-author code, tests, and specs.
- Integrates with MCP servers (including `tenxfeedbackanalytics`, `git-mcp`, and `filesystem-mcp`) to give the assistant structured access to telemetry, version control, and files.

### Context Engineering (`.cursor/rules`)

- Rules files such as `agent.mdc` and `chimera_rules.mdc` define:
  - The prime directive to **read `specs/` before generating code**.
  - Java-specific directives (Java 21+, records for DTOs, JUnit 5, clean architecture).
  - Traceability requirements: the assistant must explain plans, reference specs, and confirm assumptions before implementing changes.
- These rules ensure that every use of MCP tooling (development or runtime-focused) happens within a disciplined, spec-driven workflow.

### MCP Integration (`.cursor/mcp.json`)

- Registers MCP servers available during development, including:
  - `tenxfeedbackanalytics` for feedback logging.
  - Additional servers such as `git-mcp` and `filesystem-mcp` for version control and repository access.
- The AI assistant uses this configuration to:
  - Choose appropriate MCP tools when performing actions (e.g., inspecting diffs via `git-mcp`, editing files via `filesystem-mcp`, logging session metadata via `tenxfeedbackanalytics`).
  - Keep all interactions auditable and bounded to the Chimera repository.

Together, these elements create a coherent tooling strategy: **developer MCP tools** give the assistant safe, high-level control over the repository and its history; **runtime MCP tools** power the influencer agent swarm in production; and the **Cursor + rules + MCP configuration** glue everything together into a tightly controlled, spec-first development environment.


