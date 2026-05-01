package com.chimera.policy;

import com.chimera.llm.LlmClient;
import com.chimera.orchestrator.PipelineRequest;
import com.chimera.persistence.RunHistory;
import com.chimera.persistence.RunRecord;
import com.chimera.trend.Trend;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A trend selector that asks an LLM to pick the best trend.
 *
 * Same interface, different brain: we hand it the situation (available trends,
 * goal, used trends) as a prompt; it returns one trend topic. The selector
 * then validates the LLM's choice against the available list -- the LLM
 * cannot smuggle a hallucinated topic past the policy.
 */
public class LlmTrendSelector implements TrendSelector {

    private static final String NONE_TOKEN = "NONE";

    private final LlmClient llm;

    public LlmTrendSelector(LlmClient llm) {
        this.llm = llm;
    }

    @Override
    public Optional<Trend> select(List<Trend> available, PipelineRequest goal, RunHistory history) {
        if (available.isEmpty()) {
            return Optional.empty();
        }

        Set<String> usedTopics = history.findByCategory(goal.category()).stream()
                .map(RunRecord::result)
                .flatMap(r -> r.selectedTrend().stream())
                .map(Trend::topic)
                .collect(Collectors.toSet());

        String prompt = buildPrompt(available, goal, usedTopics);
        String chosen = llm.complete(prompt).trim();

        if (chosen.isEmpty() || chosen.equalsIgnoreCase(NONE_TOKEN)) {
            return Optional.empty();
        }

        // Validate: the LLM's choice must match one of the available trends.
        // If it hallucinates a topic, we return empty -- not the hallucinated
        // value. The agent's safety doesn't depend on the LLM behaving well.
        return available.stream()
                .filter(t -> t.topic().equalsIgnoreCase(chosen))
                .findFirst();
    }

    private String buildPrompt(List<Trend> available, PipelineRequest goal, Set<String> used) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are a content strategist for an autonomous AI social media influencer.\n\n");

        sb.append("CONTEXT:\n")
                .append("- Platform: ").append(goal.platform()).append("\n")
                .append("- Category: ").append(goal.category()).append("\n")
                .append("- Persona: ").append(goal.characterReferenceId()).append("\n\n");

        sb.append("AVAILABLE TRENDS (topic, engagement score 0-1):\n");
        for (Trend t : available) {
            sb.append("- ").append(t.topic())
                    .append(" (").append(t.engagementScore()).append(")\n");
        }

        sb.append("\nRECENTLY USED TRENDS (do not repeat these):\n");
        if (used.isEmpty()) {
            sb.append("- (none)\n");
        } else {
            for (String topic : used) {
                sb.append("- ").append(topic).append("\n");
            }
        }

        sb.append("\nINSTRUCTIONS:\n")
                .append("- Pick the single best unused trend, considering engagement and persona fit.\n")
                .append("- Respond with ONLY the trend topic, exactly as written above.\n")
                .append("- Do not add quotes, punctuation, or explanations.\n")
                .append("- If no good option exists, respond with: ").append(NONE_TOKEN).append("\n\n");

        sb.append("YOUR CHOICE: ");

        return sb.toString();
    }
}
