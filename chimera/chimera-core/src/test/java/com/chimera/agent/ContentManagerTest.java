package com.chimera.agent;

import com.chimera.content.MockContentGenerator;
import com.chimera.orchestrator.PipelineRequest;
import com.chimera.persistence.InMemoryRunHistory;
import com.chimera.persistence.RunHistory;
import com.chimera.policy.NaiveBudgetPolicy;
import com.chimera.policy.NaiveTrendSelector;
import com.chimera.policy.StrictVerdictPolicy;
import com.chimera.publisher.MockPlatformPublisher;
import com.chimera.publisher.PublishStatus;
import com.chimera.trend.MockTrendFetcher;
import com.chimera.verifier.ContentVerifier;
import com.chimera.verifier.MockContentVerifier;
import com.chimera.verifier.VerificationIssue;
import com.chimera.verifier.VerificationRequest;
import com.chimera.verifier.VerificationResult;
import com.chimera.verifier.Verdict;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ContentManager including the agentic feature that's actually new:
 * the Worker -> Judge -> Worker revision loop.
 */
class ContentManagerTest {

    private final PipelineRequest goal =
            new PipelineRequest("bluesky", "fitness", "persona-1", 5.00);

    @Test
    void managerRunsOneCycleAndPublishesOnHappyPath() {
        var history = new InMemoryRunHistory();
        ContentManager manager = buildManager(history, new MockContentVerifier(), 1, 0);

        ManagerResult result = manager.run(goal);

        assertEquals(1, result.requested());
        assertEquals(1, result.published());
        assertEquals(1, result.cycles().size());
        assertEquals(PublishStatus.PUBLISHED,
                result.cycles().get(0).publishResult().orElseThrow().status());
    }

    @Test
    void managerRunsMultipleCyclesUpToTarget() {
        var history = new InMemoryRunHistory();
        ContentManager manager = buildManager(history, new MockContentVerifier(), 3, 0);

        ManagerResult result = manager.run(goal);

        assertEquals(3, result.requested());
        assertEquals(3, result.cycles().size());
    }

    @Test
    void revisionLoopRetriesUpToMaxAndThenAccepts() {
        // A Judge that returns REVISE the first time, APPROVE the second time.
        // Proves the loop fires and the Worker is called again with feedback.
        AtomicInteger judgeCallCount = new AtomicInteger();
        ContentVerifier alternatingVerifier = new ContentVerifier() {
            @Override
            public VerificationResult verify(VerificationRequest request) {
                int call = judgeCallCount.incrementAndGet();
                if (call == 1) {
                    return new VerificationResult(
                            request.contentId(),
                            Verdict.REVISE,
                            List.of(new VerificationIssue("FIX_ME", "low", "needs polish")),
                            0.6
                    );
                }
                return new VerificationResult(request.contentId(), Verdict.APPROVE, List.of(), 0.95);
            }
        };

        var history = new InMemoryRunHistory();
        ContentManager manager = buildManager(history, alternatingVerifier, 1, 2);

        ManagerResult result = manager.run(goal);

        assertEquals(1, result.published(), "should eventually publish after revision");
        assertEquals(1, result.cycles().get(0).revisionsUsed(), "should record one revision");
        assertEquals(2, judgeCallCount.get(), "judge should be called twice (initial + after revision)");
    }

    @Test
    void revisionLoopGivesUpAfterMaxRetries() {
        // Always-REVISE Judge with maxRevisions=1 -> we hit the cap and stop.
        ContentVerifier alwaysRevise = request -> new VerificationResult(
                request.contentId(),
                Verdict.REVISE,
                List.of(new VerificationIssue("X", "low", "always wants more")),
                0.5
        );

        var history = new InMemoryRunHistory();
        ContentManager manager = buildManager(history, alwaysRevise, 1, 1);

        ManagerResult result = manager.run(goal);

        assertEquals(0, result.published(), "should not publish if Judge never approves");
        assertEquals(1, result.cycles().get(0).revisionsUsed(), "should have used the max revision");
        assertTrue(result.cycles().get(0).stoppedReason().isPresent());
    }

    private ContentManager buildManager(
            RunHistory history,
            ContentVerifier verifier,
            int targetPosts,
            int maxRevisions
    ) {
        Worker worker = new ContentWorker(
                new MockTrendFetcher(),
                new NaiveTrendSelector(),
                new MockContentGenerator(),
                history
        );
        Judge judge = new ContentJudge(verifier);

        return new ContentManager(
                worker, judge,
                new MockPlatformPublisher(),
                new NaiveBudgetPolicy(),
                new StrictVerdictPolicy(),
                history,
                DraftStore.NONE,
                targetPosts, maxRevisions
        );
    }
}
