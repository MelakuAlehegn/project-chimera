package com.chimera.policy;

import com.chimera.orchestrator.PipelineRequest;
import com.chimera.persistence.RunHistory;
import com.chimera.persistence.RunRecord;
import com.chimera.trend.Trend;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A policy with memory: skip trends already used in this category, then pick
 * the highest-engagement trend from what remains.
 *
 * This is the smallest example of "intelligence" in the system: the same input
 * (list of trends) produces different outputs depending on what the agent
 * has done before.
 */
public class MemoryAwareTrendSelector implements TrendSelector {

    @Override
    public Optional<Trend> select(List<Trend> available, PipelineRequest goal, RunHistory history) {
        // Build the set of trend topics already used in this category.
        Set<String> usedTopics = history.findByCategory(goal.category()).stream()
                .map(RunRecord::result)
                .flatMap(r -> r.selectedTrend().stream())
                .map(Trend::topic)
                .collect(Collectors.toSet());

        // Filter out used trends, then pick the one with highest engagement.
        return available.stream()
                .filter(t -> !usedTopics.contains(t.topic()))
                .max(Comparator.comparingDouble(Trend::engagementScore));
    }
}
