package com.chimera.orchestrator;

import com.chimera.content.MockContentGenerator;
import com.chimera.persistence.InMemoryRunHistory;
import com.chimera.publisher.MockPlatformPublisher;
import com.chimera.publisher.PublishStatus;
import com.chimera.trend.MockTrendFetcher;
import com.chimera.verifier.MockContentVerifier;
import com.chimera.verifier.Verdict;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test: run all four mock skills through the pipeline.
 *
 * This is the first time the four skills meet each other. The test verifies
 * data flows step-to-step and the final publish result is captured.
 */
class ContentPipelineTest {

    @Test
    void pipelineShouldRunAllFourStepsAndPublishOnHappyPath() {
        var pipeline = new ContentPipeline(
                new MockTrendFetcher(),
                new MockContentGenerator(),
                new MockContentVerifier(),
                new MockPlatformPublisher(),
                new InMemoryRunHistory()
        );

        var goal = new PipelineRequest("tiktok", "fitness", "fit_chimera_v1", 5.00);

        PipelineResult result = pipeline.run(goal);

        assertTrue(result.selectedTrend().isPresent(), "Should pick a trend.");
        assertTrue(result.generatedContent().isPresent(), "Should generate content.");
        assertTrue(result.verificationResult().isPresent(), "Should verify content.");
        assertTrue(result.publishResult().isPresent(), "Should publish.");
        assertTrue(result.stoppedReason().isEmpty(), "Happy path should not have a stop reason.");

        assertEquals(Verdict.APPROVE, result.verificationResult().get().verdict());
        assertEquals(PublishStatus.PUBLISHED, result.publishResult().get().status());
    }

    @Test
    void pipelineShouldRecordEveryRunInHistory() {
        var history = new InMemoryRunHistory();
        var pipeline = new ContentPipeline(
                new MockTrendFetcher(),
                new MockContentGenerator(),
                new MockContentVerifier(),
                new MockPlatformPublisher(),
                history
        );

        pipeline.run(new PipelineRequest("tiktok", "fitness", "fit_chimera_v1", 5.00));
        pipeline.run(new PipelineRequest("tiktok", "fitness", "fit_chimera_v1", 0.50));  // budget fail

        assertEquals(2, history.findAll().size(),
                "History should record both successful and failed runs.");
        assertEquals(2, history.findByCategory("fitness").size(),
                "Both fitness runs should be findable by category.");
    }

    @Test
    void pipelineShouldStopWhenBudgetIsInsufficient() {
        var pipeline = new ContentPipeline(
                new MockTrendFetcher(),
                new MockContentGenerator(),
                new MockContentVerifier(),
                new MockPlatformPublisher(),
                new InMemoryRunHistory()
        );

        var goal = new PipelineRequest("tiktok", "fitness", "fit_chimera_v1", 0.50);

        PipelineResult result = pipeline.run(goal);

        assertTrue(result.selectedTrend().isPresent(), "Trend was fetched before budget check.");
        assertTrue(result.generatedContent().isEmpty(), "Generation should have been blocked.");
        assertTrue(result.publishResult().isEmpty(), "Nothing should have been published.");
        assertTrue(result.stoppedReason().isPresent());
        assertTrue(result.stoppedReason().get().contains("budget"),
                "Stop reason should mention budget.");
    }
}
