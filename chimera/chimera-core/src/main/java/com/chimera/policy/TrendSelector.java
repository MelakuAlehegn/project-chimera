package com.chimera.policy;

import com.chimera.orchestrator.PipelineRequest;
import com.chimera.persistence.RunHistory;
import com.chimera.trend.Trend;

import java.util.List;
import java.util.Optional;

/**
 * Decision policy: given a list of available trends, the agent's goal, and its
 * memory of past runs, choose which trend to use -- or none.
 *
 * Implementations can be rule-based (this package) or LLM-backed (later).
 * The pipeline depends on this interface, not on any concrete strategy.
 */
public interface TrendSelector {

    Optional<Trend> select(
            List<Trend> available,
            PipelineRequest goal,
            RunHistory history
    );
}
