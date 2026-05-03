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
        TrendResponse trendResponse = trendFetcher.fetchTrends(
                new TrendRequest(goal.platform(), goal.category())
        );
        if (trendResponse.trends().isEmpty()) {
            return Optional.empty();
        }

        Optional<Trend> selected = trendSelector.select(trendResponse.trends(), goal, runHistory);
        if (selected.isEmpty()) {
            return Optional.empty();
        }

        Trend trend = selected.get();
        GeneratedContent content = contentGenerator.generate(new ContentGenerationRequest(
                trend.topic(),
                goal.characterReferenceId(),
                goal.budget(),
                goal.platform()
        ));

        return Optional.of(new Candidate(trend, content));
    }

    @Override
    public Optional<Candidate> revise(
            PipelineRequest goal,
            Candidate previous,
            List<VerificationIssue> feedback
    ) throws BudgetExceededException {
        // Re-use the same trend, regenerate with feedback baked into the topic
        // string so the underlying ContentGenerator's prompt picks it up.
        String topicWithFeedback = previous.selectedTrend().topic()
                + " (revision required: " + summarize(feedback) + ")";

        GeneratedContent revised = contentGenerator.generate(new ContentGenerationRequest(
                topicWithFeedback,
                goal.characterReferenceId(),
                goal.budget(),
                goal.platform()
        ));

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
