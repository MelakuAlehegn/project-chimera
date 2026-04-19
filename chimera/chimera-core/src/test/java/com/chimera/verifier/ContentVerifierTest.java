package com.chimera.verifier;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract and behavior tests for the ContentVerifier skill.
 */
class ContentVerifierTest {

    // --- Structural contract ---

    @Test
    void contentVerifierShouldBeAnInterfaceWithVerifyMethod() {
        Class<?> verifierInterface = ContentVerifier.class;

        assertTrue(verifierInterface.isInterface(), "ContentVerifier must be defined as an interface.");

        Method verify = Arrays.stream(verifierInterface.getMethods())
                .filter(m -> m.getName().equals("verify"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("ContentVerifier must declare a verify(...) method."));

        assertEquals(VerificationRequest.class, verify.getParameterTypes()[0],
                "verify(...) should accept a VerificationRequest.");
        assertEquals(VerificationResult.class, verify.getReturnType(),
                "verify(...) should return a VerificationResult.");
    }

    @Test
    void verdictShouldBeAnEnumWithApproveReviseReject() {
        assertTrue(Verdict.class.isEnum(), "Verdict must be an enum.");

        var values = Arrays.stream(Verdict.values()).map(Enum::name).toList();
        assertTrue(values.contains("APPROVE"), "Verdict must include APPROVE.");
        assertTrue(values.contains("REVISE"), "Verdict must include REVISE.");
        assertTrue(values.contains("REJECT"), "Verdict must include REJECT.");
    }

    @Test
    void verificationResultRecordShouldMatchOutputContract() {
        assertTrue(VerificationResult.class.isRecord(), "VerificationResult must be a Java 21 record.");

        var names = Arrays.stream(VerificationResult.class.getRecordComponents())
                .map(c -> c.getName()).toList();

        assertTrue(names.contains("contentId"));
        assertTrue(names.contains("verdict"));
        assertTrue(names.contains("issues"));
        assertTrue(names.contains("safetyScore"));
    }

    // --- Behavior (via MockContentVerifier) ---

    @Test
    void verifyShouldApproveContentWithANonEmptyScript() {
        ContentVerifier verifier = new MockContentVerifier();
        var request = new VerificationRequest(
                "draft_001",
                "Hook... Main... CTA.",
                "Check this out",
                "tiktok"
        );

        VerificationResult result = verifier.verify(request);

        assertEquals(Verdict.APPROVE, result.verdict(), "Non-empty script should be approved.");
        assertEquals("draft_001", result.contentId(), "Result must echo the contentId.");
        assertNotNull(result.issues(), "issues list must not be null.");
        assertTrue(result.issues().isEmpty(), "Approved content should have no issues.");
        assertTrue(result.safetyScore() > 0.9, "Approved content should have a high safety score.");
    }

    @Test
    void verifyShouldRejectContentWithAnEmptyScript() {
        ContentVerifier verifier = new MockContentVerifier();
        var request = new VerificationRequest(
                "draft_002",
                "",
                "Check this out",
                "tiktok"
        );

        VerificationResult result = verifier.verify(request);

        assertEquals(Verdict.REJECT, result.verdict(), "Empty script should be rejected.");
        assertFalse(result.issues().isEmpty(), "Rejected content must report at least one issue.");
        assertEquals("EMPTY_SCRIPT", result.issues().get(0).code(),
                "The reported issue should be EMPTY_SCRIPT.");
    }
}
