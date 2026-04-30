package com.chimera.policy;

import com.chimera.verifier.VerificationResult;
import com.chimera.verifier.Verdict;

/**
 * Publish only when the verifier explicitly approves.
 */
public class StrictVerdictPolicy implements VerdictPolicy {

    @Override
    public boolean shouldPublish(VerificationResult verification) {
        return verification.verdict() == Verdict.APPROVE;
    }
}
