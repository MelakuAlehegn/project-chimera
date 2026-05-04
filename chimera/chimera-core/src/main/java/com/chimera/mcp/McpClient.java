package com.chimera.mcp;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Minimal Model Context Protocol client.
 *
 * Lifecycle:
 *   1. construct (typically spawns the server subprocess)
 *   2. initialize() -- handshake; must be called before any tool calls
 *   3. listTools() / callTool(...) any number of times
 *   4. close() -- terminates the server
 *
 * Implementations are not required to be thread-safe.
 */
public interface McpClient extends AutoCloseable {

    void initialize();

    List<McpTool> listTools();

    JsonNode callTool(String name, JsonNode arguments);

    @Override
    void close();
}
