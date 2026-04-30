package com.chimera.orchestrator;

import com.chimera.content.MockContentGenerator;
import com.chimera.persistence.InMemoryRunHistory;
import com.chimera.policy.BudgetPolicy;
import com.chimera.policy.DailyCapBudgetPolicy;
import com.chimera.policy.MemoryAwareTrendSelector;
import com.chimera.policy.NaiveBudgetPolicy;
import com.chimera.policy.NaiveTrendSelector;
import com.chimera.policy.StrictVerdictPolicy;
import com.chimera.policy.TrendSelector;
import com.chimera.policy.VerdictPolicy;
import com.chimera.publisher.MockPlatformPublisher;
import com.chimera.publisher.PublishStatus;
import com.chimera.trend.MockTrendFetcher;
import com.chimera.verifier.MockContentVerifier;
import com.chimera.verifier.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end pipeline tests with different policies plugged in.
 *
 * Each test mixes-and-matches policies to demonstrate that the pipeline's
 * behavior changes based on which policies are injected, not on its own code.
 */
class ContentPipelineTest {

    private ContentPipeline buildPipeline(
            InMemoryRunHistory history,
            TrendSelector selector,
            BudgetPolicy budgetPolicy,
            VerdictPolicy verdictPolicy
    ) {
        return new ContentPipeline(
                new MockTrendFetcher(),
                new MockContentGenerator(),
                new MockContentVerifier(),
                new MockPlatformPublisher(),
                history,
                selector,
                budgetPolicy,
                verdictPolicy
        );
    }

    @Test
    void pipelineShouldRunAllFourStepsAndPublishOnHappyPath() {
        var pipeline = buildPipeline(
                new InMemoryRunHistory(),
                new NaiveTrendSelector(),
                new NaiveBudgetPolicy(),
                new StrictVerdictPolicy()
        );
        var goal = new PipelineRequest("tiktok", "fitness", "fit_chimera_v1", 5.00);

        PipelineResult result = pipeline.run(goal);

        assertTrue(result.publishResult().isPresent());
        assertEquals(Verdict.APPROVE, result.verificationResult().get().verdict());
        assertEquals(PublishStatus.PUBLISHED, result.publishResult().get().status());
    }

    @Test
    void pipelineShouldRecordEveryRunInHistory() {
        var history = new InMemoryRunHistory();
        var pipeline = buildPipeline(
                history,
                new NaiveTrendSelector(),
                new NaiveBudgetPolicy(),
                new StrictVerdictPolicy()
        );

        pipeline.run(new PipelineRequest("tiktok", "fitness", "fit_chimera_v1", 5.00));
        pipeline.run(new PipelineRequest("tiktok", "fitness", "fit_chimera_v1", 0.50));

        assertEquals(2, history.findAll().size());
    }

    @Test
    void memoryAwarePipelineShouldNotRepeatTrendsAcrossRuns() {
        var pipeline = buildPipeline(
                new InMemoryRunHistory(),
                new MemoryAwareTrendSelector(),
                new NaiveBudgetPolicy(),
                new StrictVerdictPolicy()
        );
        var goal = new PipelineRequest("tiktok", "fitness", "fit_chimera_v1", 5.00);

        var topic1 = pipeline.run(goal).selectedTrend().orElseThrow().topic();
        var topic2 = pipeline.run(goal).selectedTrend().orElseThrow().topic();

        assertNotEquals(topic1, topic2);
    }

    /**
     * The agentic budget test: a daily cap of $7 means we can run twice with
     * $5 budgets (sum=$10 > 7) but the SECOND run gets denied by the policy.
     */
    @Test
    void dailyBudgetCapShouldDenyOnceTotalExceedsCap() {
        var history = new InMemoryRunHistory();
        var pipeline = buildPipeline(
                history,
                new NaiveTrendSelector(),
                new DailyCapBudgetPolicy(7.00),
                new StrictVerdictPolicy()
        );
        var goal = new PipelineRequest("tiktok", "fitness", "fit_chimera_v1", 5.00);

        PipelineResult run1 = pipeline.run(goal);
        PipelineResult run2 = pipeline.run(goal);

        assertTrue(run1.publishResult().isPresent(), "First run within cap -> publishes.");
        assertTrue(run2.publishResult().isEmpty(), "Second run exceeds cap -> blocked.");
        assertTrue(run2.stoppedReason().orElse("").contains("budget policy"),
                "Stop reason should reference the budget policy.");
    }
}
