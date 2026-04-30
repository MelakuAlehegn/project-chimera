package com.chimera.persistence;

import com.chimera.content.GeneratedContent;
import com.chimera.orchestrator.PipelineRequest;
import com.chimera.orchestrator.PipelineResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RunHistoryTest {

    @Test
    void saveAndFindAllShouldRoundTrip() {
        RunHistory history = new InMemoryRunHistory();
        var record = sampleRun("run-1", "fitness", "content-1");

        history.save(record);

        assertEquals(1, history.findAll().size());
        assertEquals("run-1", history.findAll().get(0).runId());
    }

    @Test
    void findByContentIdShouldLocateRunsThatProducedContent() {
        RunHistory history = new InMemoryRunHistory();
        history.save(sampleRun("run-1", "fitness", "content-A"));
        history.save(sampleRun("run-2", "fitness", "content-B"));

        Optional<RunRecord> found = history.findByContentId("content-B");

        assertTrue(found.isPresent());
        assertEquals("run-2", found.get().runId());
    }

    @Test
    void findByCategoryShouldFilterByGoalCategory() {
        RunHistory history = new InMemoryRunHistory();
        history.save(sampleRun("run-1", "fitness", "content-A"));
        history.save(sampleRun("run-2", "cooking", "content-B"));
        history.save(sampleRun("run-3", "fitness", "content-C"));

        assertEquals(2, history.findByCategory("fitness").size());
        assertEquals(1, history.findByCategory("cooking").size());
    }

    private RunRecord sampleRun(String runId, String category, String contentId) {
        var goal = new PipelineRequest("tiktok", category, "persona-1", 5.00);
        var content = new GeneratedContent(contentId, "script", "caption", "tiktok");
        var result = new PipelineResult(
                Optional.empty(),
                Optional.of(content),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
        return new RunRecord(runId, Instant.now(), goal, result);
    }
}
