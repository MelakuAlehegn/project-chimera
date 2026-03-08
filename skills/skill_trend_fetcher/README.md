## Skill: TrendFetcher

### Purpose

The `TrendFetcher` skill discovers high-signal social media trends for downstream content generation.  
Manager and Worker agents use this skill to query trend APIs (as MCP tools) and obtain structured trend data that drives the autonomous influencer workflow.

---

### Input Contract

The skill wraps a runtime MCP tool aligned with the `TrendFetcher` API defined in `specs/technical.md`.

**Request shape (logical schema):**

```json
{
  "platform": "tiktok",        // social platform identifier
  "category": "fitness",       // content category or niche
  "limit": 20,                 // optional: max number of trends to return
  "minEngagementScore": 0.5    // optional: filter for sufficiently strong trends
}
```

**Key fields:**
- `platform` (string, required): Target social platform (e.g., `"tiktok"`).
- `category` (string, required): Content category or niche (e.g., `"fitness"`).
- `limit` (integer, optional): Maximum number of trends to retrieve.
- `minEngagementScore` (number, optional): Lower bound on trend engagement.

---

### Output Contract

The response must match and extend the structure from `specs/technical.md`.

**Response shape (logical schema):**

```json
{
  "trends": [
    {
      "topic": "morning workout routine",
      "engagementScore": 0.89,
      "platform": "tiktok",
      "category": "fitness",
      "fetchedAt": "2026-03-08T12:34:56Z"
    }
  ]
}
```

**Key fields:**
- `trends` (array, required): List of discovered trend objects.
  - `topic` (string, required): Human-readable topic label.
  - `engagementScore` (number, required): Normalized score from 0–1 as in `specs/technical.md`.
  - `platform` (string, required): Echo of the input platform.
  - `category` (string, required): Echo of the input category.
  - `fetchedAt` (ISO-8601 timestamp, required): Time when the trend was retrieved.

---

### Dependencies

- **Runtime MCP Tools**
  - A trend API MCP tool that implements the `TrendFetcher` contract from `specs/technical.md`.
- **Data Layer**
  - Optional: tools or services that persist selected trends into the `Trends` table:
    - `id`, `platform`, `topic`, `engagement_score`.
- **Agent Capabilities**
  - Worker agents with `trend_analysis` capability (as advertised in OpenClaw messages) typically invoke this skill.

---

### Possible Failure Modes

- **Upstream API errors**
  - Trend API is unavailable, rate-limited, or returns a non-200 status.
  - Malformed or partial responses that do not match the expected schema.
- **Invalid input**
  - Unsupported `platform` value or empty/invalid `category`.
  - Non-sensical limits (e.g., negative or extremely large `limit`).
- **Low-signal results**
  - All returned `engagementScore` values fall below `minEngagementScore`, resulting in an effectively empty `trends` list.
- **Data consistency issues**
  - Duplicate topics or conflicting engagement scores when persisting to the `Trends` table.
  - Desynchronization between stored trends and the latest API results.

