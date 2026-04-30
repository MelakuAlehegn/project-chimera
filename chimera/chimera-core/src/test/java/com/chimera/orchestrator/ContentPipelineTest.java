package com.chimera.orchestrator;

import com.chimera.content.MockContentGenerator;
import com.chimera.persistence.InMemoryRunHistory;
import com.chimera.policy.MemoryAwareTrendSelector;
import com.chimera.policy.NaiveTrendSelector;
import com.chimera.policy.TrendSelector;
import com.chimera.publisher.MockPlatformPublisher;
import com.chimera.publisher.PublishStatus;
import com.chimera.trend.MockTrendFetcher;
import com.chimera.verifier.MockContentVerifier;
import com.chimera.verifier.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end pipeline tests with different policies plugged in.
 */
class ContentPipelineTest {

    private ContentPipeline buildPipeline(InMemoryRunHistory history, TrendSelector selector) {
        return new ContentPipeline(
                new MockTrendFetcher(),
                new MockContentGenerator(),
                new MockContentVerifier(),
                new MockPlatformPublisher(),
                history,
                selector
        );
    }

    @Test
    void pipelineShouldRunAllFourStepsAndPublishOnHappyPath() {
        var pipeline = buildPipeline(new InMemoryRunHistory(), new NaiveTrendSelector());
        var goal = new PipelineRequest("tiktok", "fitness", "fit_chimera_v1", 5.00);

        PipelineResult result = pipeline.run(goal);

        assertTrue(result.selectedTrend().isPresent());
        assertTrue(result.generatedContent().isPresent());
        assertTrue(result.verificationResult().isPresent());
        assertTrue(result.publishResult().isPresent());
        assertTrue(result.stoppedReason().isEmpty());

        assertEquals(Verdict.APPROVE, result.verificationResult().get().verdict());
        assertEquals(PublishStatus.PUBLISHED, result.publishResult().get().status());
    }

    @Test
    void pipelineShouldRecordEveryRunInHistory() {
        var history = new InMemoryRunHistory();
        var pipeline = buildPipeline(history, new NaiveTrendSelector());

        pipeline.run(new PipelineRequest("tiktok", "fitness", "fit_chimera_v1", 5.00));
        pipeline.run(new PipelineRequest("tiktok", "fitness", "fit_chimera_v1", 0.50));

        assertEquals(2, history.findAll().size());
        assertEquals(2, history.findByCategory("fitness").size());
    }

    @Test
    void pipelineShouldStopWhenBudgetIsInsufficient() {
        var pipeline = buildPipeline(new InMemoryRunHistory(), new NaiveTrendSelector());
        var goal = new PipelineRequest("tiktok", "fitness", "fit_chimera_v1", 0.50);

        PipelineResult result = pipeline.run(goal);

        assertTrue(result.selectedTrend().isPresent());
        assertTrue(result.generatedContent().isEmpty());
        assertTrue(result.publishResult().isEmpty());
        assertTrue(result.stoppedReason().isPresent());
        assertTrue(result.stoppedReason().get().contains("budget"));
    }

    /**
     * The agentic test: run twice with MemoryAwareTrendSelector.
     * The second run should pick a different trend than the first because
     * the first one is now in memory.
     */
    @Test
    void memoryAwarePipelineShouldNotRepeatTrendsAcrossRuns() {
        var history = new InMemoryRunHistory();
        var pipeline = buildPipeline(history, new MemoryAwareTrendSelector());
        var goal = new PipelineRequest("tiktok", "fitness", "fit_chimera_v1", 5.00);

        PipelineResult run1 = pipeline.run(goal);
        PipelineResult run2 = pipeline.run(goal);

        assertTrue(run1.selectedTrend().isPresent());
        assertTrue(run2.selectedTrend().isPresent());

        var topic1 = run1.selectedTrend().get().topic();
        var topic2 = run2.selectedTrend().get().topic();

        assertNotEquals(topic1, topic2,
                "The memory-aware selector should not pick the same trend twice in a row.");
    }
}
