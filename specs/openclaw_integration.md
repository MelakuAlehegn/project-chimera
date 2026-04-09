# OpenClaw Integration Specification

## Overview

Chimera agents publish their **availability** and **capabilities** to the [OpenClaw](https://openclaw.org) agent social network. This allows external agents in the ecosystem to discover Chimera workers, request collaboration, and coordinate multi-agent workflows across organizational boundaries.

---

## Protocol: Agent Availability Message

Each Chimera agent periodically broadcasts a structured availability message to OpenClaw.

### Message Schema

```json
{
  "agent_id": "chimera_agent_1",
  "status": "available",
  "capabilities": ["trend_analysis", "video_generation"],
  "metadata": {
    "system": "project_chimera",
    "version": "1.0",
    "region": "us-east-1"
  },
  "heartbeat_interval_seconds": 30
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `agent_id` | String | Yes | Unique identifier for the agent instance |
| `status` | String | Yes | One of: `available`, `busy`, `offline` |
| `capabilities` | String[] | Yes | List of skill identifiers the agent can execute |
| `metadata` | Object | No | System-level metadata for discovery and routing |
| `heartbeat_interval_seconds` | int | No | How often the agent sends heartbeats (default: 30) |

### Supported Capabilities

| Capability ID | Maps to Skill | Description |
|---|---|---|
| `trend_analysis` | `skill_trend_fetcher` | Discover trending topics from social platforms |
| `video_generation` | `skill_content_generator` | Generate video scripts and captions |
| `content_verification` | `skill_content_verifier` | Evaluate content for safety and quality |
| `publishing` | `skill_platform_publisher` | Publish approved content to platforms |

---

## Status Lifecycle

```
  +-> available --+
  |               |
  |               v
offline <---- busy
  ^               |
  |               |
  +----- ---------+
```

- **available**: Agent is ready to accept work via OpenClaw task requests.
- **busy**: Agent is currently executing a skill. Not accepting new tasks.
- **offline**: Agent has shut down or missed heartbeats. OpenClaw marks the agent as offline after 3 missed heartbeats.

---

## Integration Points

### Outbound (Chimera -> OpenClaw)

1. **Registration**: On startup, each agent sends an initial availability message with `status: "available"`.
2. **Heartbeat**: Agents send periodic heartbeats to confirm they are still active.
3. **Status transitions**: When an agent picks up a task, it broadcasts `status: "busy"`. On completion, it returns to `"available"`.

### Inbound (OpenClaw -> Chimera)

1. **Task requests**: External agents can request Chimera agents to execute skills by referencing their `agent_id` and desired `capability`.
2. **Discovery queries**: External systems query OpenClaw to find available Chimera agents with specific capabilities.

---

## Security Considerations

- Agent IDs must not expose internal infrastructure details.
- Capability lists should only advertise skills the agent is authorized to execute.
- Heartbeat messages must be authenticated to prevent spoofing.
- The integration does not expose raw MCP tool endpoints to external agents; skills act as the public interface.
