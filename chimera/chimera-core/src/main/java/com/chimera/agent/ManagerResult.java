package com.chimera.agent;

import com.chimera.publisher.PublishResult;
import com.chimera.verifier.VerificationResult;

import java.util.List;
import java.util.Optional;

/**
 * Summary of one Manager.run() invocation.
 *
 * A Manager may publish multiple posts in a single run (or zero, if budget
 * runs out). This record captures aggregate counters and the per-cycle
 * trace so humans can audit what the agent did.
 */
public record ManagerResult(
        int requested,
        int published,
        int rejected,
        int errored,
        List<CycleTrace> cycles
) {

    /**
     * Per-cycle outcome. Each cycle is one "produce -> judge -> publish?" attempt.
     */
    public record CycleTrace(
            Optional<Candidate> candidate,
            Optional<VerificationResult> verification,
            Optional<PublishResult> publishResult,
            int revisionsUsed,
            Optional<String> stoppedReason
    ) {
    }
}
