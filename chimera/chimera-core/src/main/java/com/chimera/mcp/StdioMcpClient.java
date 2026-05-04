package com.chimera.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP client that talks to a server subprocess over stdio.
 *
 * Wire protocol: JSON-RPC 2.0, one message per line, UTF-8.
 *   request:      { "jsonrpc":"2.0", "id":1, "method":"...", "params":{...} }
 *   response:     { "jsonrpc":"2.0", "id":1, "result":{...} }       (success)
 *                 { "jsonrpc":"2.0", "id":1, "error":{...} }        (failure)
 *   notification: { "jsonrpc":"2.0",         "method":"..." }       (no id)
 *
 * Synchronous request/response model: each request blocks until the matching
 * id arrives. Notifications from the server (e.g. log messages) are silently
 * ignored.
 */
public class StdioMcpClient implements McpClient {

    private static final Logger log = LoggerFactory.getLogger(StdioMcpClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Process process;
    private final BufferedWriter stdin;
    private final BufferedReader stdout;
    private final AtomicLong messageId = new AtomicLong(1);

    /**
     * Spawn the given command as an MCP server subprocess.
     *
     * @param command the executable + arguments, e.g.
     *                ["npx", "-y", "@modelcontextprotocol/server-filesystem", "/tmp/chimera-drafts"]
     */
    public StdioMcpClient(List<String> command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);  // stderr separate; we don't read it for now
            this.process = pb.start();
            this.stdin = new BufferedWriter(new OutputStreamWriter(
                    process.getOutputStream(), StandardCharsets.UTF_8));
            this.stdout = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8));
            log.info("MCP server started: {}", String.join(" ", command));
        } catch (IOException e) {
            throw new McpException("Failed to start MCP server: " + command, e);
        }
    }

    // --- protocol entry points ---

    @Override
    public void initialize() {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("protocolVersion", "2024-11-05");
        params.set("capabilities", MAPPER.createObjectNode());
        ObjectNode info = params.putObject("clientInfo");
        info.put("name", "chimera");
        info.put("version", "0.1");

        request("initialize", params);
        // Per spec, follow up with the initialized notification (no id, no response).
        notify("notifications/initialized");
        log.info("MCP server initialized");
    }

    @Override
    public List<McpTool> listTools() {
        JsonNode result = request("tools/list", MAPPER.createObjectNode());
        JsonNode toolsNode = result.path("tools");

        List<McpTool> tools = new ArrayList<>();
        for (JsonNode t : toolsNode) {
            tools.add(new McpTool(
                    t.path("name").asText(),
                    t.path("description").asText(""),
                    t.path("inputSchema")
            ));
        }
        log.info("MCP server exposes {} tool(s)", tools.size());
        return tools;
    }

    @Override
    public JsonNode callTool(String name, JsonNode arguments) {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("name", name);
        params.set("arguments", arguments);

        log.info("MCP tool call: {} (args={})", name, arguments);
        JsonNode result = request("tools/call", params);

        // Servers can signal an error inside a successful response via isError=true
        if (result.path("isError").asBoolean(false)) {
            throw new McpException("Tool '" + name + "' returned isError: " + result);
        }
        return result;
    }

    @Override
    public void close() {
        try {
            stdin.close();
            stdout.close();
        } catch (IOException ignored) {
            // best-effort cleanup
        }
        process.destroy();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("MCP server closed");
    }

    // --- JSON-RPC plumbing ---

    private JsonNode request(String method, JsonNode params) {
        long id = messageId.getAndIncrement();
        ObjectNode req = MAPPER.createObjectNode();
        req.put("jsonrpc", "2.0");
        req.put("id", id);
        req.put("method", method);
        req.set("params", params);

        send(req);
        return awaitResponse(id);
    }

    private void notify(String method) {
        ObjectNode req = MAPPER.createObjectNode();
        req.put("jsonrpc", "2.0");
        req.put("method", method);
        send(req);
    }

    private void send(ObjectNode message) {
        try {
            stdin.write(MAPPER.writeValueAsString(message));
            stdin.write('\n');
            stdin.flush();
        } catch (IOException e) {
            throw new McpException("Failed to send MCP message", e);
        }
    }

    /**
     * Read lines from the server until a response with the matching id arrives.
     * Notifications and other ids are silently skipped.
     */
    private JsonNode awaitResponse(long id) {
        try {
            String line;
            while ((line = stdout.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                JsonNode message = MAPPER.readTree(line);

                JsonNode idField = message.get("id");
                if (idField == null) {
                    continue;  // server-emitted notification, ignore
                }
                if (idField.asLong() != id) {
                    continue;  // response to some other request, ignore (shouldn't happen in sync mode)
                }

                JsonNode error = message.get("error");
                if (error != null && !error.isNull()) {
                    throw new McpException("MCP server returned error: " + error.toString());
                }
                JsonNode result = message.get("result");
                if (result == null) {
                    throw new McpException("MCP response missing 'result': " + line);
                }
                return result;
            }
            throw new McpException("MCP server closed stdout before responding to id=" + id);
        } catch (IOException e) {
            throw new McpException("Failed to read MCP response", e);
        }
    }

}
