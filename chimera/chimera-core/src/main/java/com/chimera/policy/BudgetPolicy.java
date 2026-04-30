package com.chimera.policy;

import com.chimera.orchestrator.PipelineRequest;
import com.chimera.persistence.RunHistory;

import java.util.Optional;

/**
 * Decides whether a requested budget is approved.
 * Returns the approved amount (possibly reduced), or empty to deny.
 */
public interface BudgetPolicy {

    Optional<Double> approve(double requestedBudget, PipelineRequest goal, RunHistory history);
}
