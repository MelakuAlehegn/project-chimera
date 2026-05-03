package com.chimera.agent;

import com.chimera.verifier.VerificationResult;

/**
 * Critical role: evaluates a Candidate, returns a structured verdict.
 *
 * The Judge does not know the goal -- it only evaluates the artifact.
 * That separation is what lets us reuse the same Judge for multiple
 * Worker output types in the future.
 */
public interface Judge {

    VerificationResult evaluate(Candidate candidate);
}
