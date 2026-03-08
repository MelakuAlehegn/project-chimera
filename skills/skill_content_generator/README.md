## Skill: ContentGenerator

### Purpose

The `ContentGenerator` skill transforms trend insights and persona context into draft social media content (scripts, captions, outlines).  
Worker agents use this skill after `TrendFetcher` to generate influencer-ready material tailored to specific platforms and audiences.

---

### Input Contract

The skill wraps one or more content-generation MCP tools (e.g., script and caption generators).

**Request shape (logical schema):**

```json
{
  "trend": {
    "topic": "morning workout routine",
    "engagementScore": 0.89,
    "platform": "tiktok",
    "category": "fitness"
  },
  "persona": {
    "name": "FitChimera",
    "voice": "supportive, energetic, science-backed"
  },
  "contentType": "short_video_script",
  "targetPlatform": "tiktok",
  "constraints": {
    "maxDurationSeconds": 60,
    "callToAction": "follow_for_more_tips"
  }
}
```

**Key fields:**
- `trend` (object, required): Single trend selected from `TrendFetcher` output.
- `persona` (object, required): High-level influencer persona info.
- `contentType` (string, required): e.g., `"short_video_script"`, `"caption"`, `"thread"`.
- `targetPlatform` (string, required): Social platform the content is intended for.
- `constraints` (object, optional): Length, tone, and CTA constraints.

---

### Output Contract

The response should provide structured content plus metadata suitable for storage in the `Videos` table or equivalent.

**Response shape (logical schema):**

```json
{
  "contentId": "draft_video_001",
  "trendTopic": "morning workout routine",
  "script": "Intro hook...\nMain content...\nCall to action...",
  "caption": "Transform your mornings with this simple routine 💪",
  "targetPlatform": "tiktok",
  "estimatedDurationSeconds": 55,
  "metadata": {
    "personaName": "FitChimera",
    "language": "en",
    "safetyFlags": [],
    "version": 1
  }
}
```

**Key fields:**
- `contentId` (string, required): Logical identifier for the generated draft.
- `trendTopic` (string, required): Copy of the trend topic for traceability.
- `script` (string, required for video content): Main textual script body.
- `caption` (string, optional): Platform-optimized caption.
- `targetPlatform` (string, required): Platform for which content was generated.
- `estimatedDurationSeconds` (integer, optional): Approximate runtime for video content.
- `metadata` (object, required): Persona and language info, early safety flags, versioning.

---

### Dependencies

- **Upstream Skills**
  - `TrendFetcher` (provides the `trend` input).
- **Runtime MCP Tools**
  - LLM-based content generation services for scripts and captions.
  - Optional style-transfer or rewriting tools for persona/brand alignment.
- **Data Layer**
  - Tools that persist generated content into the `Videos` table:
    - `id` (mapped from `contentId`), `trend_id`, `script`, `publish_status`.
- **Agent Capabilities**
  - Worker agents with `video_generation` capability typically invoke this skill.

---

### Possible Failure Modes

- **Weak or ambiguous prompts**
  - Poorly specified `persona` or `constraints` lead to off-brand or generic content.
- **Over-long or under-specified outputs**
  - Content exceeds `maxDurationSeconds` or lacks required structural elements (e.g., missing hook or CTA).
- **Safety or compliance issues**
  - Generated content may violate platform policies or brand guidelines (later caught by `ContentVerifier`).
- **Data persistence errors**
  - Failures when storing `script` or related metadata into the `Videos` table.
- **Drift from input trend**
  - Content that no longer clearly maps back to the original `trend.topic`, reducing traceability and effectiveness.

