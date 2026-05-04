package com.chimera.mcp;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Description of a tool exposed by an MCP server.
 *
 * The server returns these from tools/list. The agent decides which to call.
 * inputSchema is the JSON Schema the server expects for arguments.
 */
public record McpTool(
        String name,
        String description,
        JsonNode inputSchema
) {
}
