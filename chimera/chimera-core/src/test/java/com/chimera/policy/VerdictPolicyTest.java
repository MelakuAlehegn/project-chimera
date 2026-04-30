package com.chimera.policy;

import com.chimera.verifier.VerificationResult;
import com.chimera.verifier.Verdict;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VerdictPolicyTest {

    private VerificationResult resultWith(Verdict verdict) {
        return new VerificationResult("c-1", verdict, List.of(), 1.0);
    }

    @Test
    void strictPolicyOnlyPublishesOnApprove() {
        var policy = new StrictVerdictPolicy();

        assertTrue(policy.shouldPublish(resultWith(Verdict.APPROVE)));
        assertFalse(policy.shouldPublish(resultWith(Verdict.REVISE)));
        assertFalse(policy.shouldPublish(resultWith(Verdict.REJECT)));
    }

    @Test
    void permissivePolicyPublishesOnApproveAndRevise() {
        var policy = new PermissiveVerdictPolicy();

        assertTrue(policy.shouldPublish(resultWith(Verdict.APPROVE)));
        assertTrue(policy.shouldPublish(resultWith(Verdict.REVISE)));
        assertFalse(policy.shouldPublish(resultWith(Verdict.REJECT)));
    }
}
