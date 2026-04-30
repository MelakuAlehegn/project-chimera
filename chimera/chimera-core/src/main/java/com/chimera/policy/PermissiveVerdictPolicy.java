package com.chimera.policy;

import com.chimera.verifier.VerificationResult;
import com.chimera.verifier.Verdict;

/**
 * Publish on APPROVE or REVISE -- treat REVISE as "good enough for now".
 * REJECT still blocks. Useful when we want throughput over polish.
 */
public class PermissiveVerdictPolicy implements VerdictPolicy {

    @Override
    public boolean shouldPublish(VerificationResult verification) {
        return verification.verdict() == Verdict.APPROVE
                || verification.verdict() == Verdict.REVISE;
    }
}
