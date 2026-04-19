package com.chimera.verifier;

import java.util.List;

/**
 * Mock implementation of ContentVerifier for development and testing.
 *
 * Returns APPROVE with a high safety score and no issues for any non-empty
 * script. Returns REJECT for empty or missing scripts.
 * No external moderation API calls are made.
 */
public class MockContentVerifier implements ContentVerifier {

    private static final double APPROVED_SAFETY_SCORE = 0.95;

    @Override
    public VerificationResult verify(VerificationRequest request) {
        if (request.script() == null || request.script().isBlank()) {
            var issue = new VerificationIssue(
                    "EMPTY_SCRIPT",
                    "high",
                    "Script is empty or blank and cannot be published."
            );
            return new VerificationResult(
                    request.contentId(),
                    Verdict.REJECT,
                    List.of(issue),
                    0.0
            );
        }

        return new VerificationResult(
                request.contentId(),
                Verdict.APPROVE,
                List.of(),
                APPROVED_SAFETY_SCORE
        );
    }
}
