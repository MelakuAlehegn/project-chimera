package com.chimera.content;

import com.chimera.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;

/**
 * Real ContentGenerator backed by an LLM.
 *
 * Sends a single structured prompt asking for JSON output, then parses
 * the response into a GeneratedContent. The LlmClient is provider-agnostic
 * (Gemini today, anything else tomorrow).
 *
 * Production guardrails:
 *   - Budget below MIN_BUDGET throws BudgetExceededException before any API call.
 *   - Malformed LLM output throws IllegalStateException -- prefer failing visibly
 *     over publishing broken content.
 */
public class LlmContentGenerator implements ContentGenerator {

    private static final double MIN_BUDGET = 0.50;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LlmClient llm;

    public LlmContentGenerator(LlmClient llm) {
        this.llm = llm;
    }

    @Override
    public GeneratedContent generate(ContentGenerationRequest req) throws BudgetExceededException {
        if (req.budget() < MIN_BUDGET) {
            throw new BudgetExceededException(
                    "Budget %.2f is below the minimum %.2f for LLM content generation."
                            .formatted(req.budget(), MIN_BUDGET));
        }

        String prompt = buildPrompt(req);
        String raw = llm.complete(prompt);

        return parse(raw, req.targetPlatform());
    }

    private String buildPrompt(ContentGenerationRequest req) {
        return """
                You are a content writer for an autonomous AI social media influencer.

                CONTEXT:
                - Topic: %s
                - Persona ID: %s
                - Target platform: %s

                TASK:
                Write a short-form post for the target platform about the topic.
                Match the persona (assume they are upbeat, science-backed, and concise).
                Keep the script under 250 words. Keep the caption under 280 characters.

                OUTPUT FORMAT:
                Respond with ONLY a single JSON object on one line, no surrounding prose,
                no markdown, no code fences. Use this exact shape:

                {"script": "<the script body>", "caption": "<the platform caption>"}

                BEGIN OUTPUT:
                """.formatted(req.topic(), req.characterReferenceId(), req.targetPlatform());
    }

    private GeneratedContent parse(String raw, String targetPlatform) {
        String cleaned = stripCodeFences(raw).trim();
        try {
            JsonNode node = MAPPER.readTree(cleaned);
            String script = textField(node, "script");
            String caption = textField(node, "caption");
            return new GeneratedContent(
                    "llm-" + UUID.randomUUID(),
                    script,
                    caption,
                    targetPlatform
            );
        } catch (IOException e) {
            throw new IllegalStateException(
                    "LLM returned non-JSON output: " + cleaned, e);
        }
    }

    /** LLMs sometimes wrap JSON in ```json ... ``` despite instructions. Tolerate it. */
    private String stripCodeFences(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            // remove first line (```json or ```) and trailing ```
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed;
    }

    private String textField(JsonNode node, String name) {
        JsonNode field = node.get(name);
        if (field == null || !field.isTextual()) {
            throw new IllegalStateException(
                    "LLM JSON missing required field '" + name + "': " + node);
        }
        return field.asText();
    }
}
