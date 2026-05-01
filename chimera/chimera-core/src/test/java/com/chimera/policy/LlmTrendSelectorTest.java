package com.chimera.policy;

import com.chimera.llm.LlmClient;
import com.chimera.orchestrator.PipelineRequest;
import com.chimera.persistence.InMemoryRunHistory;
import com.chimera.trend.Trend;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the LlmTrendSelector's logic with a stub LlmClient.
 *
 * No real API calls. We verify how the selector handles the LLM's output --
 * exact match, whitespace, NONE, and hallucinated topics.
 */
class LlmTrendSelectorTest {

    private final PipelineRequest goal =
            new PipelineRequest("tiktok", "fitness", "persona-1", 5.00);

    private final List<Trend> trends = List.of(
            new Trend("alpha", 0.5),
            new Trend("beta", 0.9),
            new Trend("gamma", 0.7)
    );

    @Test
    void picksTrendWhenLlmReturnsExactMatch() {
        LlmClient stub = prompt -> "beta";
        var selector = new LlmTrendSelector(stub);

        Optional<Trend> picked = selector.select(trends, goal, new InMemoryRunHistory());

        assertEquals("beta", picked.orElseThrow().topic());
    }

    @Test
    void picksTrendIgnoringWhitespaceAndCase() {
        LlmClient stub = prompt -> "  Beta  \n";
        var selector = new LlmTrendSelector(stub);

        Optional<Trend> picked = selector.select(trends, goal, new InMemoryRunHistory());

        assertEquals("beta", picked.orElseThrow().topic());
    }

    @Test
    void returnsEmptyWhenLlmReturnsNoneToken() {
        LlmClient stub = prompt -> "NONE";
        var selector = new LlmTrendSelector(stub);

        Optional<Trend> picked = selector.select(trends, goal, new InMemoryRunHistory());

        assertTrue(picked.isEmpty());
    }

    @Test
    void returnsEmptyWhenLlmHallucinatesTopic() {
        LlmClient stub = prompt -> "delta";  // not in the available list
        var selector = new LlmTrendSelector(stub);

        Optional<Trend> picked = selector.select(trends, goal, new InMemoryRunHistory());

        assertTrue(picked.isEmpty(),
                "Selector must reject any topic not in the available list -- LLM safety guardrail.");
    }

    @Test
    void promptIncludesUsedTrendsFromHistory() {
        // Capture whatever prompt the LLM receives to assert its content.
        StringBuilder capturedPrompt = new StringBuilder();
        LlmClient stub = prompt -> {
            capturedPrompt.append(prompt);
            return "alpha";
        };

        var history = new InMemoryRunHistory();
        // Save a run that "used" beta in the same category.
        history.save(new com.chimera.persistence.RunRecord(
                "r-1",
                java.time.Instant.now(),
                goal,
                new com.chimera.orchestrator.PipelineResult(
                        Optional.of(new Trend("beta", 0.9)),
                        Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty()
                )
        ));

        var selector = new LlmTrendSelector(stub);
        selector.select(trends, goal, history);

        assertTrue(capturedPrompt.toString().contains("beta"),
                "Prompt should list 'beta' under recently used trends.");
    }
}
