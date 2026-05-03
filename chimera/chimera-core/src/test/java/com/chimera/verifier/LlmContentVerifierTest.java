package com.chimera.verifier;

import com.chimera.llm.LlmClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LlmContentVerifier using a stub LlmClient.
 *
 * No real API calls. Verifies parsing logic, verdict mapping, issue handling,
 * and the safety property: parse failures and unknown verdicts must throw,
 * never silently approve.
 */
class LlmContentVerifierTest {

    private final VerificationRequest request = new VerificationRequest(
            "c-1", "the script body", "the caption", "bluesky"
    );

    @Test
    void parsesApproveVerdict() {
        LlmClient stub = prompt -> """
                {"verdict":"APPROVE","safetyScore":0.92,"issues":[]}
                """;
        var verifier = new LlmContentVerifier(stub);

        VerificationResult result = verifier.verify(request);

        assertEquals(Verdict.APPROVE, result.verdict());
        assertEquals(0.92, result.safetyScore());
        assertTrue(result.issues().isEmpty());
        assertEquals("c-1", result.contentId());
    }

    @Test
    void parsesReviseWithIssues() {
        LlmClient stub = prompt -> """
                {"verdict":"REVISE","safetyScore":0.55,
                 "issues":[
                   {"code":"UNVERIFIED_HEALTH_CLAIM","severity":"high","message":"Claim about cortisol unsupported"}
                 ]}
                """;
        var verifier = new LlmContentVerifier(stub);

        VerificationResult result = verifier.verify(request);

        assertEquals(Verdict.REVISE, result.verdict());
        assertEquals(1, result.issues().size());
        assertEquals("UNVERIFIED_HEALTH_CLAIM", result.issues().get(0).code());
        assertEquals("high", result.issues().get(0).severity());
    }

    @Test
    void verdictIsCaseInsensitive() {
        LlmClient stub = prompt -> """
                {"verdict":"approve","safetyScore":0.8}
                """;
        var verifier = new LlmContentVerifier(stub);

        assertEquals(Verdict.APPROVE, verifier.verify(request).verdict());
    }

    @Test
    void toleratesCodeFences() {
        LlmClient stub = prompt -> """
                ```json
                {"verdict":"REJECT","safetyScore":0.1,"issues":[]}
                ```
                """;
        var verifier = new LlmContentVerifier(stub);

        assertEquals(Verdict.REJECT, verifier.verify(request).verdict());
    }

    @Test
    void clampsSafetyScoreOutsideRange() {
        LlmClient stub = prompt -> """
                {"verdict":"APPROVE","safetyScore":1.7,"issues":[]}
                """;
        var verifier = new LlmContentVerifier(stub);

        assertEquals(1.0, verifier.verify(request).safetyScore(),
                "safetyScore > 1 should clamp to 1");
    }

    @Test
    void missingIssuesArrayIsAllowed() {
        LlmClient stub = prompt -> """
                {"verdict":"APPROVE","safetyScore":0.9}
                """;
        var verifier = new LlmContentVerifier(stub);

        assertTrue(verifier.verify(request).issues().isEmpty());
    }

    @Test
    void unknownVerdictThrows() {
        LlmClient stub = prompt -> """
                {"verdict":"MAYBE","safetyScore":0.5,"issues":[]}
                """;
        var verifier = new LlmContentVerifier(stub);

        assertThrows(IllegalStateException.class, () -> verifier.verify(request),
                "Unknown verdict should halt the pipeline -- never silently approve.");
    }

    @Test
    void malformedJsonThrows() {
        LlmClient stub = prompt -> "this is not JSON";
        var verifier = new LlmContentVerifier(stub);

        assertThrows(IllegalStateException.class, () -> verifier.verify(request));
    }

    @Test
    void missingVerdictFieldThrows() {
        LlmClient stub = prompt -> """
                {"safetyScore":0.9,"issues":[]}
                """;
        var verifier = new LlmContentVerifier(stub);

        assertThrows(IllegalStateException.class, () -> verifier.verify(request));
    }
}
