package com.chimera.orchestrator;

import com.chimera.content.BudgetExceededException;
import com.chimera.content.ContentGenerationRequest;
import com.chimera.content.ContentGenerator;
import com.chimera.content.GeneratedContent;
import com.chimera.persistence.RunHistory;
import com.chimera.persistence.RunRecord;
import com.chimera.policy.BudgetPolicy;
import com.chimera.policy.TrendSelector;
import com.chimera.policy.VerdictPolicy;
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

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Sequential pipeline that runs all four skills in order:
 *   trend -> generate -> verify -> publish
 *
 * Decisions are delegated to injected policies (TrendSelector, BudgetPolicy,
 * VerdictPolicy). The pipeline itself is dumb glue: it only orchestrates.
 */
public class ContentPipeline {

    private final TrendFetcher trendFetcher;
    private final ContentGenerator contentGenerator;
    private final ContentVerifier contentVerifier;
    private final PlatformPublisher publisher;
    private final RunHistory runHistory;
    private final TrendSelector trendSelector;
    private final BudgetPolicy budgetPolicy;
    private final VerdictPolicy verdictPolicy;

    public ContentPipeline(
            TrendFetcher trendFetcher,
            ContentGenerator contentGenerator,
            ContentVerifier contentVerifier,
            PlatformPublisher publisher,
            RunHistory runHistory,
            TrendSelector trendSelector,
            BudgetPolicy budgetPolicy,
            VerdictPolicy verdictPolicy
    ) {
        this.trendFetcher = trendFetcher;
        this.contentGenerator = contentGenerator;
        this.contentVerifier = contentVerifier;
        this.publisher = publisher;
        this.runHistory = runHistory;
        this.trendSelector = trendSelector;
        this.budgetPolicy = budgetPolicy;
        this.verdictPolicy = verdictPolicy;
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
        var trendRequest = new TrendRequest(goal.platform(), goal.category());
        TrendResponse trendResponse = trendFetcher.fetchTrends(trendRequest);

        if (trendResponse.trends().isEmpty()) {
            return stopped("no trends returned for " + goal.category());
        }

        // DECISION POINT: which trend? -> TrendSelector
        Optional<Trend> selected = trendSelector.select(trendResponse.trends(), goal, runHistory);
        if (selected.isEmpty()) {
            return stopped("trend selector returned no choice (all trends already used?)");
        }
        Trend selectedTrend = selected.get();

        // DECISION POINT: budget approval -> BudgetPolicy
        Optional<Double> approvedBudget = budgetPolicy.approve(goal.budget(), goal, runHistory);
        if (approvedBudget.isEmpty()) {
            return stoppedAfterTrend(selectedTrend, "budget policy denied request (cap exceeded?)");
        }

        // === STEP 2: Generate content ===
        var contentRequest = new ContentGenerationRequest(
                selectedTrend.topic(),
                goal.characterReferenceId(),
                approvedBudget.get(),
                goal.platform()
        );

        GeneratedContent generated;
        try {
            generated = contentGenerator.generate(contentRequest);
        } catch (BudgetExceededException e) {
            return stoppedAfterTrend(selectedTrend, "budget exceeded at generator: " + e.getMessage());
        }

        // === STEP 3: Verify content ===
        var verificationRequest = new VerificationRequest(
                generated.contentId(),
                generated.script(),
                generated.caption(),
                generated.targetPlatform()
        );
        VerificationResult verification = contentVerifier.verify(verificationRequest);

        // DECISION POINT: should we publish? -> VerdictPolicy
        if (!verdictPolicy.shouldPublish(verification)) {
            return stoppedAfterVerification(selectedTrend, generated, verification);
        }

        // === STEP 4: Publish ===
        var publishRequest = new PublishRequest(
                generated.contentId(),
                generated.script(),
                generated.caption(),
                generated.targetPlatform()
        );
        PublishResult publishResult = publisher.publish(publishRequest);

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
                Optional.of("verdict policy rejected publish (verdict was " + verification.verdict() + ")")
        );
    }
}
