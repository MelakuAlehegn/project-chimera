package com.chimera.policy;

import com.chimera.verifier.VerificationResult;

/**
 * Decides whether a verifier's verdict is good enough to proceed to publishing.
 *
 * Different policies allow different risk appetites: a strict policy publishes
 * only on APPROVE; a permissive policy also publishes on REVISE; a future
 * RetryOnRevisePolicy could trigger regeneration with feedback.
 */
public interface VerdictPolicy {

    boolean shouldPublish(VerificationResult verification);
}
