package com.chimera.verifier;

/**
 * Skill interface for verifying generated content against safety, quality,
 * and policy rules.
 *
 * Implementations may call moderation APIs, policy engines, or LLM judges.
 * See specs/functional.md (US-3.1) for the behavioral contract.
 */
public interface ContentVerifier {

    VerificationResult verify(VerificationRequest request);
}
