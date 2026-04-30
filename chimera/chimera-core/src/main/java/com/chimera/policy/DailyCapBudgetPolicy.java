package com.chimera.policy;

import com.chimera.orchestrator.PipelineRequest;
import com.chimera.persistence.RunHistory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Denies a request if the sum of budgets requested in the last 24 hours
 * (including this one) would exceed the configured daily cap.
 *
 * Demonstrates the same memory-driven decision pattern as
 * MemoryAwareTrendSelector, applied to a different concern.
 */
public class DailyCapBudgetPolicy implements BudgetPolicy {

    private final double dailyCap;

    public DailyCapBudgetPolicy(double dailyCap) {
        this.dailyCap = dailyCap;
    }

    @Override
    public Optional<Double> approve(double requestedBudget, PipelineRequest goal, RunHistory history) {
        Instant since = Instant.now().minus(Duration.ofHours(24));

        double spentInWindow = history.findAll().stream()
                .filter(r -> r.runAt().isAfter(since))
                .mapToDouble(r -> r.goal().budget())
                .sum();

        if (spentInWindow + requestedBudget > dailyCap) {
            return Optional.empty();
        }
        return Optional.of(requestedBudget);
    }
}
