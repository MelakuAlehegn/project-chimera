package com.chimera.policy;

import com.chimera.content.GeneratedContent;
import com.chimera.orchestrator.PipelineRequest;
import com.chimera.orchestrator.PipelineResult;
import com.chimera.persistence.InMemoryRunHistory;
import com.chimera.persistence.RunHistory;
import com.chimera.persistence.RunRecord;
import com.chimera.trend.Trend;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for trend selection policies.
 *
 * Selectors are pure decisions: same input -> same output. This makes them
 * trivial to test without spinning up the full pipeline.
 */
class TrendSelectorTest {

    private final PipelineRequest goal =
            new PipelineRequest("tiktok", "fitness", "persona-1", 5.00);

    @Test
    void naiveSelectorPicksFirstTrend() {
        var trends = List.of(
                new Trend("alpha", 0.5),
                new Trend("beta", 0.9)
        );

        Optional<Trend> picked = new NaiveTrendSelector()
                .select(trends, goal, new InMemoryRunHistory());

        assertEquals("alpha", picked.orElseThrow().topic());
    }

    @Test
    void naiveSelectorReturnsEmptyForNoTrends() {
        Optional<Trend> picked = new NaiveTrendSelector()
                .select(List.of(), goal, new InMemoryRunHistory());

        assertTrue(picked.isEmpty());
    }

    @Test
    void memoryAwareSelectorPicksHighestEngagementWhenNothingUsed() {
        var trends = List.of(
                new Trend("alpha", 0.5),
                new Trend("beta", 0.9),
                new Trend("gamma", 0.7)
        );

        Optional<Trend> picked = new MemoryAwareTrendSelector()
                .select(trends, goal, new InMemoryRunHistory());

        assertEquals("beta", picked.orElseThrow().topic(),
                "Should pick the highest-engagement trend.");
    }

    @Test
    void memoryAwareSelectorSkipsAlreadyUsedTrends() {
        var trends = List.of(
                new Trend("alpha", 0.5),
                new Trend("beta", 0.9),   // highest, but used
                new Trend("gamma", 0.7)
        );
        RunHistory history = new InMemoryRunHistory();
        history.save(runRecordWithTrend("beta"));

        Optional<Trend> picked = new MemoryAwareTrendSelector()
                .select(trends, goal, history);

        assertEquals("gamma", picked.orElseThrow().topic(),
                "beta is in memory; should pick the next best (gamma).");
    }

    @Test
    void memoryAwareSelectorReturnsEmptyWhenAllTrendsUsed() {
        var trends = List.of(new Trend("alpha", 0.5), new Trend("beta", 0.9));
        RunHistory history = new InMemoryRunHistory();
        history.save(runRecordWithTrend("alpha"));
        history.save(runRecordWithTrend("beta"));

        Optional<Trend> picked = new MemoryAwareTrendSelector()
                .select(trends, goal, history);

        assertTrue(picked.isEmpty(), "All trends used -> no choice possible.");
    }

    private RunRecord runRecordWithTrend(String topic) {
        var trend = new Trend(topic, 0.0);
        var content = new GeneratedContent("c-" + topic, "s", "c", "tiktok");
        var result = new PipelineResult(
                Optional.of(trend),
                Optional.of(content),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
        return new RunRecord("run-" + topic, Instant.now(), goal, result);
    }
}
