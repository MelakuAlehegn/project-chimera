# Chimera Agent Skills

Skills are reusable capabilities that Worker and Judge agents invoke at runtime. Each skill wraps one or more MCP tools and defines a clear Input/Output contract.

## Skill Inventory

| Skill | Directory | Agent Type | Purpose |
|---|---|---|---|
| **TrendFetcher** | `skill_trend_fetcher/` | Worker | Discover trending topics from social media APIs |
| **ContentGenerator** | `skill_content_generator/` | Worker | Generate video scripts and captions from trends |
| **ContentVerifier** | `skill_content_verifier/` | Judge | Evaluate content for safety, quality, and brand alignment |
| **PlatformPublisher** | `skill_platform_publisher/` | Worker | Publish approved content to social media platforms |

## Pipeline Flow

```
TrendFetcher -> ContentGenerator -> ContentVerifier -> PlatformPublisher
```

Each skill's `README.md` defines:
- **Input Contract** -- JSON schema and key fields
- **Output Contract** -- response shape and field descriptions
- **Dependencies** -- upstream skills, MCP tools, and data layer
- **Failure Modes** -- what can go wrong and how

## Conventions

- Skills do NOT contain implementation logic in this phase -- only contracts.
- Each skill maps to one or more Java types in `chimera/chimera-core` (records, interfaces).
- Skill contracts must align with `specs/technical.md` API definitions.
