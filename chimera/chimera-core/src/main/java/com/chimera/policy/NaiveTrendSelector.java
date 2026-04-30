package com.chimera.policy;

import com.chimera.orchestrator.PipelineRequest;
import com.chimera.persistence.RunHistory;
import com.chimera.trend.Trend;

import java.util.List;
import java.util.Optional;

/**
 * The dumbest possible policy: pick the first available trend.
 * Ignores memory and goal. Useful as a baseline and for tests where
 * intelligence is not under test.
 */
public class NaiveTrendSelector implements TrendSelector {

    @Override
    public Optional<Trend> select(List<Trend> available, PipelineRequest goal, RunHistory history) {
        return available.isEmpty() ? Optional.empty() : Optional.of(available.get(0));
    }
}
