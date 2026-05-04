package com.chimera.mcp;

/**
 * Raised when the MCP client cannot complete a protocol exchange.
 *
 * Wraps both transport failures (subprocess died, IO error) and protocol
 * failures (server returned an error response, malformed JSON-RPC message).
 */
public class McpException extends RuntimeException {

    public McpException(String message) {
        super(message);
    }

    public McpException(String message, Throwable cause) {
        super(message, cause);
    }
}
