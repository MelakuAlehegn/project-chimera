package com.chimera.llm;

import com.chimera.config.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Google Gemini implementation of LlmClient.
 *
 * Talks to the v1beta generateContent endpoint over plain HTTPS.
 * No SDK -- using Java's built-in HttpClient and Jackson for JSON.
 *
 * The full API surface is much richer (chat history, tool calls, streaming);
 * for trend selection a single-shot completion is all we need.
 */
public class GeminiLlmClient implements LlmClient {

    private static final String ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);

    private final HttpClient http;
    private final String apiKey;
    private final String model;

    public GeminiLlmClient(Config config) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.apiKey = config.geminiApiKey();
        this.model = config.geminiModel();
    }

    @Override
    public String complete(String prompt) {
        String url = ENDPOINT_TEMPLATE.formatted(model, apiKey);
        String body = buildRequestBody(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        long startMs = System.currentTimeMillis();
        log.info("Calling LLM ({} prompt chars, model={})", prompt.length(), model);

        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsedMs = System.currentTimeMillis() - startMs;

            if (response.statusCode() != 200) {
                log.warn("LLM call failed in {}ms: HTTP {}", elapsedMs, response.statusCode());
                throw new IllegalStateException(
                        "Gemini returned HTTP " + response.statusCode() + ": " + response.body());
            }

            String text = extractText(response.body());
            log.info("LLM responded in {}ms ({} chars)", elapsedMs, text.length());
            return text;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Gemini HTTP call failed: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gemini HTTP call interrupted", e);
        }
    }

    private String buildRequestBody(String prompt) {
        // Gemini request shape: {"contents":[{"parts":[{"text": "..."}]}]}
        ObjectNode root = MAPPER.createObjectNode();
        root.putArray("contents")
                .addObject()
                .putArray("parts")
                .addObject()
                .put("text", prompt);
        try {
            return MAPPER.writeValueAsString(root);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize Gemini request", e);
        }
    }

    private String extractText(String responseBody) {
        // Response shape: {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            JsonNode text = root.path("candidates").path(0)
                    .path("content")
                    .path("parts").path(0)
                    .path("text");

            if (text.isMissingNode() || text.isNull()) {
                throw new IllegalStateException(
                        "Unexpected Gemini response shape: " + responseBody);
            }
            return text.asText();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Gemini response", e);
        }
    }
}
