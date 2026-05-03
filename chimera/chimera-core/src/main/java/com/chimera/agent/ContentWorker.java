package com.chimera.agent;

import com.chimera.content.BudgetExceededException;
import com.chimera.content.ContentGenerationRequest;
import com.chimera.content.ContentGenerator;
import com.chimera.content.GeneratedContent;
import com.chimera.orchestrator.PipelineRequest;
import com.chimera.persistence.RunHistory;
import com.chimera.policy.TrendSelector;
import com.chimera.trend.Trend;
import com.chimera.trend.TrendFetcher;
import com.chimera.trend.TrendRequest;
import com.chimera.trend.TrendResponse;
import com.chimera.verifier.VerificationIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Worker that creates short-form posts.
 *
 * produce(goal): fetch trends -> select one -> generate content
 * revise(goal, previous, feedback): re-generate using the same trend but
 *                                    with the Judge's issues injected as
 *                                    additional constraints in the prompt
 *
 * The Worker calls the ContentGenerator twice if revision is needed -- once
 * with the original prompt, once with the fix-it prompt.
 */
public class ContentWorker implements Worker {

    private static final Logger log = LoggerFactory.getLogger(ContentWorker.class);

    private final TrendFetcher trendFetcher;
    private final TrendSelector trendSelector;
    private final ContentGenerator contentGenerator;
    private final RunHistory runHistory;

    public ContentWorker(
            TrendFetcher trendFetcher,
            TrendSelector trendSelector,
            ContentGenerator contentGenerator,
            RunHistory runHistory
    ) {
        this.trendFetcher = trendFetcher;
        this.trendSelector = trendSelector;
        this.contentGenerator = contentGenerator;
        this.runHistory = runHistory;
    }

    @Override
    public Optional<Candidate> produce(PipelineRequest goal) throws BudgetExceededException {
        log.info("Worker: fetching trends for category={} on {}", goal.category(), goal.platform());
        TrendResponse trendResponse = trendFetcher.fetchTrends(
                new TrendRequest(goal.platform(), goal.category())
        );
        log.info("Worker: got {} trend(s)", trendResponse.trends().size());

        if (trendResponse.trends().isEmpty()) {
            log.warn("Worker: no trends returned, aborting cycle");
            return Optional.empty();
        }

        log.info("Worker: selecting a trend...");
        Optional<Trend> selected = trendSelector.select(trendResponse.trends(), goal, runHistory);
        if (selected.isEmpty()) {
            log.warn("Worker: selector returned no choice (all used?), aborting cycle");
            return Optional.empty();
        }

        Trend trend = selected.get();
        log.info("Worker: selected '{}' (engagement={})", trend.topic(), trend.engagementScore());

        log.info("Worker: generating content...");
        GeneratedContent content = contentGenerator.generate(new ContentGenerationRequest(
                trend.topic(),
                goal.characterReferenceId(),
                goal.budget(),
                goal.platform()
        ));
        log.info("Worker: produced candidate (contentId={})", content.contentId());

        return Optional.of(new Candidate(trend, content));
    }

    @Override
    public Optional<Candidate> revise(
            PipelineRequest goal,
            Candidate previous,
            List<VerificationIssue> feedback
    ) throws BudgetExceededException {
        log.info("Worker: revising '{}' with {} issue(s) from Judge",
                previous.selectedTrend().topic(), feedback.size());

        String topicWithFeedback = previous.selectedTrend().topic()
                + " (revision required: " + summarize(feedback) + ")";

        GeneratedContent revised = contentGenerator.generate(new ContentGenerationRequest(
                topicWithFeedback,
                goal.characterReferenceId(),
                goal.budget(),
                goal.platform()
        ));
        log.info("Worker: revised candidate (contentId={})", revised.contentId());

        return Optional.of(new Candidate(previous.selectedTrend(), revised));
    }

    private String summarize(List<VerificationIssue> feedback) {
        if (feedback.isEmpty()) {
            return "no specific issues";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < feedback.size(); i++) {
            if (i > 0) sb.append("; ");
            VerificationIssue issue = feedback.get(i);
            sb.append(issue.code()).append(": ").append(issue.message());
        }
        return sb.toString();
    }
}
