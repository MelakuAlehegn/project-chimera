## Skill: PlatformPublisher

### Purpose

The `PlatformPublisher` skill publishes approved content to external social media platforms and updates Chimera’s internal state accordingly.  
Worker agents use this skill once `ContentVerifier` has approved or revised content for a specific platform.

---

### Input Contract

The skill wraps MCP tools for platform-specific publishing APIs and any associated scheduling services.

**Request shape (logical schema):**

```json
{
  "content": {
    "contentId": "draft_video_001",
    "script": "Intro hook...\nMain content...\nCall to action...",
    "caption": "Transform your mornings with this simple routine 💪",
    "targetPlatform": "tiktok"
  },
  "publishing": {
    "scheduledTime": "2026-03-08T18:00:00Z",
    "timeZone": "UTC",
    "tags": ["fitness", "morning_routine"],
    "trackingId": "campaign_spring_2026"
  }
}
```

**Key fields:**
- `content` (object, required): Approved content bundle to publish.
- `publishing` (object, required): When and how to publish.
  - `scheduledTime` (timestamp, required): Desired publish time (immediate if near-now).
  - `timeZone` (string, required): Time zone context for scheduling.
  - `tags` (array of strings, optional): Hashtags or topic tags.
  - `trackingId` (string, optional): Campaign or experiment identifier.

---

### Output Contract

The response confirms publishing status and provides handles back to the external platform.

**Response shape (logical schema):**

```json
{
  "contentId": "draft_video_001",
  "targetPlatform": "tiktok",
  "status": "published",   // one of: scheduled, published, failed
  "platformPostId": "tt_1234567890",
  "publishedAt": "2026-03-08T18:00:02Z",
  "error": null
}
```

**Key fields:**
- `contentId` (string, required): Echo of the published content.
- `targetPlatform` (string, required): Target platform.
- `status` (string, required): `"scheduled"`, `"published"`, or `"failed"`.
- `platformPostId` (string, optional): Platform-native identifier of the created post.
- `publishedAt` (timestamp, optional): Actual publish time if successful.
- `error` (string or object, optional): Error information if `status = "failed"`.

---

### Dependencies

- **Upstream Skills**
  - `ContentGenerator` (produces drafts).
  - `ContentVerifier` (approves content for publishing).
- **Runtime MCP Tools**
  - Platform-specific publishing APIs (e.g., TikTok, Instagram, etc.).
  - Optional scheduling services if publishing is not immediate.
- **Data Layer**
  - Tools that update the `Videos` table:
    - `publish_status` transitions (e.g., `draft` → `scheduled` → `published` / `failed`).
  - Optional logging of `platformPostId` and `publishedAt` for analytics.
- **Agent Capabilities**
  - Worker agents responsible for distribution and campaign execution.

---

### Possible Failure Modes

- **Platform API failures**
  - Network errors, rate limits, or authentication/authorization problems.
- **Content-policy mismatches at publish time**
  - Platform rejects content despite prior verification (policy updates, geo-specific rules).
- **Scheduling drift**
  - Inaccurate `scheduledTime` handling due to time zone or clock skew issues.
- **Duplicate publishing**
  - Retries that result in multiple posts for the same `contentId`.
- **State sync issues**
  - `Videos.publish_status` not updated correctly, leading to confusion about whether content is live, pending, or failed.

