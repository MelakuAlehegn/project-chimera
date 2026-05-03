package com.chimera.verifier;

import com.chimera.llm.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Real ContentVerifier backed by an LLM (the "LLM-as-Judge" pattern).
 *
 * Uses the same LlmClient as the Generator but with an evaluative prompt.
 * The Judge reads the generated content and returns a structured verdict
 * with a safety score and a list of issues.
 *
 * Safety model:
 *   - On parse failure, we THROW. Defaulting to APPROVE on uncertainty would
 *     bypass the safety check -- the whole point of the Judge.
 *   - On unknown verdict, we THROW for the same reason.
 */
public class LlmContentVerifier implements ContentVerifier {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final LlmClient llm;

    public LlmContentVerifier(LlmClient llm) {
        this.llm = llm;
    }

    @Override
    public VerificationResult verify(VerificationRequest request) {
        String raw = llm.complete(buildPrompt(request));
        return parse(raw, request.contentId());
    }

    private String buildPrompt(VerificationRequest request) {
        return """
                You are a brand safety reviewer for an autonomous AI social media influencer.
                Your job is to evaluate generated content for safety and quality.

                CONTEXT:
                - Target platform: %s

                CONTENT TO REVIEW:
                Caption: %s

                Script:
                %s

                EVALUATE FOR:
                - Misleading or unverified medical/health claims
                - Off-brand language (we want upbeat, science-backed, encouraging)
                - Profanity or sensitive content unsuitable for the platform
                - Spam patterns (excessive emojis, suspicious links, all caps shouting)
                - Factual inaccuracies where checkable

                VERDICTS:
                - APPROVE: safe, on-brand, ready to publish
                - REVISE: has fixable issues; describe each in 'issues'
                - REJECT: fundamentally unsafe, off-brand, or inaccurate

                OUTPUT FORMAT:
                Respond with ONLY a single JSON object on one line, no markdown,
                no code fences, no surrounding prose. Use this exact shape:

                {"verdict":"APPROVE","safetyScore":0.0,"issues":[{"code":"...","severity":"low","message":"..."}]}

                BEGIN OUTPUT:
                """.formatted(
                request.targetPlatform(),
                request.caption(),
                request.script()
        );
    }

    private VerificationResult parse(String raw, String contentId) {
        String cleaned = stripCodeFences(raw).trim();
        try {
            JsonNode node = MAPPER.readTree(cleaned);

            Verdict verdict = parseVerdict(node);
            double safetyScore = clamp01(node.path("safetyScore").asDouble(0.5));
            List<VerificationIssue> issues = parseIssues(node);

            return new VerificationResult(contentId, verdict, issues, safetyScore);
        } catch (IOException e) {
            throw new IllegalStateException("LLM returned non-JSON output: " + cleaned, e);
        }
    }

    private Verdict parseVerdict(JsonNode node) {
        JsonNode field = node.get("verdict");
        if (field == null || !field.isTextual()) {
            throw new IllegalStateException("LLM JSON missing 'verdict' field: " + node);
        }
        try {
            return Verdict.valueOf(field.asText().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Unknown verdict -- safer to halt than to guess.
            throw new IllegalStateException(
                    "LLM returned unknown verdict '" + field.asText() + "'; expected APPROVE/REVISE/REJECT", e);
        }
    }

    private List<VerificationIssue> parseIssues(JsonNode node) {
        List<VerificationIssue> issues = new ArrayList<>();
        JsonNode arr = node.get("issues");
        if (arr == null || !arr.isArray()) {
            return issues;  // missing issues array is allowed
        }
        for (JsonNode issue : arr) {
            issues.add(new VerificationIssue(
                    issue.path("code").asText("UNSPECIFIED"),
                    issue.path("severity").asText("medium"),
                    issue.path("message").asText("")
            ));
        }
        return issues;
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
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
