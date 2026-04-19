package com.chimera.verifier;

/**
 * A single structured issue found during verification.
 *
 * Agents can act on issues via {@code code} (machine-readable) while humans
 * read {@code message}. {@code severity} is a free-form string for now
 * ("low", "medium", "high") -- can be tightened to an enum later if needed.
 */
public record VerificationIssue(
        String code,
        String severity,
        String message
) {
}
