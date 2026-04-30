package com.chimera.policy;

import com.chimera.orchestrator.PipelineRequest;
import com.chimera.persistence.RunHistory;

import java.util.Optional;

/**
 * Trusts the requested budget without question.
 */
public class NaiveBudgetPolicy implements BudgetPolicy {

    @Override
    public Optional<Double> approve(double requestedBudget, PipelineRequest goal, RunHistory history) {
        return Optional.of(requestedBudget);
    }
}
