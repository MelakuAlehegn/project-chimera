package com.chimera.trend;

import com.chimera.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Real TrendFetcher backed by an LLM.
 *
 * Asks the LLM to imagine plausible trending topics for the requested
 * platform/category. This is "good enough" for an autonomous influencer
 * proof-of-concept without needing a real social media trends API.
 *
 * Same defensive parsing pattern as LlmContentGenerator: ask for strict
 * JSON, tolerate code-fences, fail visibly on malformed output.
 */
public class LlmTrendFetcher implements TrendFetcher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LlmClient llm;

    public LlmTrendFetcher(LlmClient llm) {
        this.llm = llm;
    }

    @Override
    public TrendResponse fetchTrends(TrendRequest request) {
        String raw = llm.complete(buildPrompt(request));
        List<Trend> trends = parseTrends(raw);
        return new TrendResponse(request.platform(), request.category(), trends);
    }

    private String buildPrompt(TrendRequest request) {
        return """
                You are a social media trend analyst.

                CONTEXT:
                - Platform: %s
                - Category: %s

                TASK:
                Suggest 5 plausible trending topics in this category for the platform.
                Each topic should be short (3-8 words) and concrete.
                Estimate an engagement score between 0.0 and 1.0 reflecting how
                likely it is to perform well right now.

                OUTPUT FORMAT:
                Respond with ONLY a single JSON object on one line, no markdown,
                no code fences, no surrounding prose. Use this exact shape:

                {"trends":[{"topic":"...","engagementScore":0.0},...]}

                BEGIN OUTPUT:
                """.formatted(request.platform(), request.category());
    }

    private List<Trend> parseTrends(String raw) {
        String cleaned = stripCodeFences(raw).trim();
        try {
            JsonNode root = MAPPER.readTree(cleaned);
            JsonNode trendsNode = root.get("trends");
            if (trendsNode == null || !trendsNode.isArray()) {
                throw new IllegalStateException("LLM JSON missing 'trends' array: " + cleaned);
            }

            List<Trend> trends = new ArrayList<>();
            for (JsonNode t : trendsNode) {
                JsonNode topic = t.get("topic");
                JsonNode score = t.get("engagementScore");
                if (topic == null || !topic.isTextual() || score == null || !score.isNumber()) {
                    throw new IllegalStateException(
                            "Trend element missing topic/engagementScore: " + t);
                }
                trends.add(new Trend(topic.asText(), score.asDouble()));
            }
            return trends;
        } catch (IOException e) {
            throw new IllegalStateException("LLM returned non-JSON output: " + cleaned, e);
        }
    }

    private String stripCodeFences(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
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
}
