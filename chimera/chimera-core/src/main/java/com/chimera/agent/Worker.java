package com.chimera.agent;

import com.chimera.content.BudgetExceededException;
import com.chimera.orchestrator.PipelineRequest;
import com.chimera.verifier.VerificationIssue;

import java.util.List;
import java.util.Optional;

/**
 * Creative role: produces a Candidate, optionally given prior feedback.
 *
 * A Worker either:
 *   - produces a fresh Candidate from a goal (first attempt)
 *   - revises a previous Candidate using Judge feedback (retry attempt)
 *
 * Returns Optional.empty() if no usable trend is available; this is normal
 * agent behavior, not an error -- the Manager handles it.
 */
public interface Worker {

    Optional<Candidate> produce(PipelineRequest goal) throws BudgetExceededException;

    Optional<Candidate> revise(
            PipelineRequest goal,
            Candidate previous,
            List<VerificationIssue> feedback
    ) throws BudgetExceededException;
}
