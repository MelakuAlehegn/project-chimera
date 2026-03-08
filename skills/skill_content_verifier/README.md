## Skill: ContentVerifier

### Purpose

The `ContentVerifier` skill evaluates generated content for safety, quality, and alignment with brand and platform policies.  
Judge or QA-style agents use this skill to decide whether content should be published as-is, revised, or rejected.

---

### Input Contract

The skill wraps MCP tools for moderation, policy checking, and quality evaluation.

**Request shape (logical schema):**

```json
{
  "content": {
    "contentId": "draft_video_001",
    "script": "Intro hook...\nMain content...\nCall to action...",
    "caption": "Transform your mornings with this simple routine 💪",
    "targetPlatform": "tiktok"
  },
  "context": {
    "trendTopic": "morning workout routine",
    "personaName": "FitChimera"
  },
  "policies": {
    "brandGuidelinesId": "chimera_brand_v1",
    "platformPolicyProfile": "tiktok_default"
  }
}
```

**Key fields:**
- `content` (object, required): The generated script/caption bundle from `ContentGenerator`.
- `context` (object, required): Trend and persona information for better judgments.
- `policies` (object, required): Identifiers for brand and platform-specific policy sets.

---

### Output Contract

The response provides a structured verdict and actionable feedback.

**Response shape (logical schema):**

```json
{
  "contentId": "draft_video_001",
  "verdict": "revise",  // one of: approve, revise, reject
  "issues": [
    {
      "code": "PLATFORM_POLICY_RISK",
      "severity": "medium",
      "message": "The script references unverified health claims.",
      "location": {
        "field": "script",
        "line": 2
      }
    }
  ],
  "suggestedChanges": [
    "Remove absolute health claims and replace with general wellness language."
  ],
  "safetyScore": 0.78
}
```

**Key fields:**
- `contentId` (string, required): Echo of the evaluated content.
- `verdict` (string, required): `"approve"`, `"revise"`, or `"reject"`.
- `issues` (array, optional): Structured list of problems found.
- `suggestedChanges` (array of strings, optional): Human-readable suggestions for fixes.
- `safetyScore` (number, optional): Normalized safety/compliance score (0–1).

---

### Dependencies

- **Upstream Skills**
  - `ContentGenerator` (provides the content to be verified).
- **Runtime MCP Tools**
  - Moderation and policy-checking tools (e.g., text safety classifiers).
  - Style and tone evaluation tools for brand alignment.
- **Data Layer**
  - Optional logging of verification results linked to `Videos` and trends for audit and learning.
- **Agent Capabilities**
  - Judge or QA agents responsible for gatekeeping publishing decisions.

---

### Possible Failure Modes

- **Overly strict or lenient verdicts**
  - Mis-tuned thresholds cause excessive rejections or unsafe approvals.
- **Incomplete policy coverage**
  - Policy definitions (`brandGuidelinesId`, `platformPolicyProfile`) do not cover all relevant edge cases.
- **Ambiguous feedback**
  - `issues` and `suggestedChanges` are too vague for Worker agents to act on effectively.
- **Tool degradation or drift**
  - Moderation models become outdated as platform policies evolve, leading to inaccurate verdicts.
- **Integration errors**
  - Incorrect linkage between verification results and the underlying `Videos` or trend records, breaking auditability.

