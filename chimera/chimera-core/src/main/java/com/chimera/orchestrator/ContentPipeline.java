package com.chimera.orchestrator;

import com.chimera.content.BudgetExceededException;
import com.chimera.content.ContentGenerationRequest;
import com.chimera.content.ContentGenerator;
import com.chimera.content.GeneratedContent;
import com.chimera.persistence.RunHistory;
import com.chimera.persistence.RunRecord;
import com.chimera.publisher.PlatformPublisher;
import com.chimera.publisher.PublishRequest;
import com.chimera.publisher.PublishResult;
import com.chimera.trend.Trend;
import com.chimera.trend.TrendFetcher;
import com.chimera.trend.TrendRequest;
import com.chimera.trend.TrendResponse;
import com.chimera.verifier.ContentVerifier;
import com.chimera.verifier.VerificationRequest;
import com.chimera.verifier.VerificationResult;
import com.chimera.verifier.Verdict;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Sequential pipeline that runs all four skills in order:
 *   trend -> generate -> verify -> publish
 *
 * This is intentionally "dumb": no ranking, no retries, no decisions.
 * Every comment marked DECISION POINT is where a real agent would think.
 */
public class ContentPipeline {

    private final TrendFetcher trendFetcher;
    private final ContentGenerator contentGenerator;
    private final ContentVerifier contentVerifier;
    private final PlatformPublisher publisher;
    private final RunHistory runHistory;

    public ContentPipeline(
            TrendFetcher trendFetcher,
            ContentGenerator contentGenerator,
            ContentVerifier contentVerifier,
            PlatformPublisher publisher,
            RunHistory runHistory
    ) {
        this.trendFetcher = trendFetcher;
        this.contentGenerator = contentGenerator;
        this.contentVerifier = contentVerifier;
        this.publisher = publisher;
        this.runHistory = runHistory;
    }

    public PipelineResult run(PipelineRequest goal) {
        PipelineResult result = doRun(goal);
        runHistory.save(new RunRecord(
                UUID.randomUUID().toString(),
                Instant.now(),
                goal,
                result
        ));
        return result;
    }

    private PipelineResult doRun(PipelineRequest goal) {
        // === STEP 1: Fetch trends ===
        // ADAPTER: PipelineRequest -> TrendRequest
        var trendRequest = new TrendRequest(goal.platform(), goal.category());
        TrendResponse trendResponse = trendFetcher.fetchTrends(trendRequest);

        if (trendResponse.trends().isEmpty()) {
            return stopped("no trends returned for " + goal.category());
        }

        // DECISION POINT: which trend do we pick?
        // Today: the first one. Later: a Manager agent ranks by engagementScore,
        //         novelty, alignment with persona, etc.
        Trend selectedTrend = trendResponse.trends().get(0);

        // === STEP 2: Generate content ===
        // ADAPTER: Trend + PipelineRequest -> ContentGenerationRequest
        var contentRequest = new ContentGenerationRequest(
                selectedTrend.topic(),
                goal.characterReferenceId(),
                goal.budget()
        );

        GeneratedContent generated;
        try {
            generated = contentGenerator.generate(contentRequest);
        } catch (BudgetExceededException e) {
            // DECISION POINT: budget failure handling.
            // Today: stop with reason. Later: agent could request more budget,
            //         retry with a cheaper model, or escalate to human.
            return stoppedAfterTrend(selectedTrend, "budget exceeded: " + e.getMessage());
        }

        // === STEP 3: Verify content ===
        // ADAPTER: GeneratedContent -> VerificationRequest
        var verificationRequest = new VerificationRequest(
                generated.contentId(),
                generated.script(),
                generated.caption(),
                generated.targetPlatform()
        );
        VerificationResult verification = contentVerifier.verify(verificationRequest);

        // DECISION POINT: what to do with the verdict?
        // Today: only PUBLISH approves; everything else stops.
        // Later: REVISE -> regenerate with feedback. REJECT -> abandon. APPROVE -> publish.
        if (verification.verdict() != Verdict.APPROVE) {
            return stoppedAfterVerification(selectedTrend, generated, verification);
        }

        // === STEP 4: Publish ===
        // ADAPTER: GeneratedContent -> PublishRequest
        var publishRequest = new PublishRequest(
                generated.contentId(),
                generated.script(),
                generated.caption(),
                generated.targetPlatform()
        );
        PublishResult publishResult = publisher.publish(publishRequest);

        // === Done ===
        return new PipelineResult(
                Optional.of(selectedTrend),
                Optional.of(generated),
                Optional.of(verification),
                Optional.of(publishResult),
                Optional.empty()
        );
    }

    private PipelineResult stopped(String reason) {
        return new PipelineResult(
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(reason)
        );
    }

    private PipelineResult stoppedAfterTrend(Trend trend, String reason) {
        return new PipelineResult(
                Optional.of(trend), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(reason)
        );
    }

    private PipelineResult stoppedAfterVerification(
            Trend trend, GeneratedContent generated, VerificationResult verification
    ) {
        return new PipelineResult(
                Optional.of(trend),
                Optional.of(generated),
                Optional.of(verification),
                Optional.empty(),
                Optional.of("verdict was " + verification.verdict() + ", pipeline only publishes APPROVE")
        );
    }
}
