package com.chimera.verifier;

import java.util.List;

/**
 * Output of the ContentVerifier skill.
 *
 * {@code verdict} drives agent control flow.
 * {@code issues} is never null -- use an empty list when there are no issues.
 * {@code safetyScore} is normalized 0.0..1.0.
 */
public record VerificationResult(
        String contentId,
        Verdict verdict,
        List<VerificationIssue> issues,
        double safetyScore
) {
}
