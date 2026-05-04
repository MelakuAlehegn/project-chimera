package com.chimera.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that spawns the official filesystem MCP server and exercises
 * the full handshake + tools/list + tools/call flow.
 *
 * Requires Node + npx on PATH. The server is fetched on first run via npx.
 *
 * Each test method gets a fresh temp directory so state doesn't leak.
 */
class StdioMcpClientIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void canInitializeAndListTools(@TempDir Path tmp) {
        try (var client = startFilesystemServer(tmp)) {
            client.initialize();
            List<McpTool> tools = client.listTools();

            assertFalse(tools.isEmpty(),
                    "filesystem server should expose at least one tool");
            assertTrue(tools.stream().anyMatch(t -> t.name().equals("write_file")),
                    "filesystem server should expose 'write_file'");
        }
    }

    @Test
    void canWriteAndReadAFileViaTools(@TempDir Path tmp) throws Exception {
        try (var client = startFilesystemServer(tmp)) {
            client.initialize();

            // Write a file via the MCP write_file tool.
            ObjectNode writeArgs = MAPPER.createObjectNode();
            writeArgs.put("path", tmp.resolve("hello.txt").toString());
            writeArgs.put("content", "hello from chimera");
            JsonNode writeResult = client.callTool("write_file", writeArgs);
            assertNotNull(writeResult);

            // Verify on disk -- this proves the tool actually ran.
            String onDisk = Files.readString(tmp.resolve("hello.txt"));
            assertEquals("hello from chimera", onDisk);
        }
    }

    private StdioMcpClient startFilesystemServer(Path allowedDir) {
        return new StdioMcpClient(List.of(
                "npx", "-y",
                "@modelcontextprotocol/server-filesystem",
                allowedDir.toString()
        ));
    }
}
