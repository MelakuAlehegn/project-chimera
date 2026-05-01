package com.chimera.llm;

/**
 * Provider-agnostic interface for an LLM completion call.
 * Send a prompt, receive a text response.
 *
 * Implementations may use Gemini, OpenAI, Claude, a local model, or a stub
 * for testing. Anything that consumes an LlmClient depends only on this
 * contract.
 */
public interface LlmClient {

    String complete(String prompt);
}
