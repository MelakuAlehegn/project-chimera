package com.chimera.persistence;

import com.chimera.content.GeneratedContent;
import com.chimera.orchestrator.PipelineRequest;
import com.chimera.orchestrator.PipelineResult;
import com.chimera.publisher.PublishResult;
import com.chimera.publisher.PublishStatus;
import com.chimera.trend.Trend;
import com.chimera.verifier.VerificationResult;
import com.chimera.verifier.Verdict;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round-trip tests for JSON serialization.
 *
 * If these pass, our records survive a trip through JSONB and back unchanged.
 */
class JsonTest {

    @Test
    void roundTripPipelineRequest() {
        var original = new PipelineRequest("tiktok", "fitness", "persona-1", 5.00);

        String json = Json.toJson(original);
        PipelineRequest restored = Json.fromJson(json, PipelineRequest.class);

        assertEquals(original, restored);
    }

    @Test
    void roundTripFullPipelineResult() {
        Instant now = Instant.parse("2026-04-19T12:34:56.789Z");

        var result = new PipelineResult(
                Optional.of(new Trend("morning workout", 0.89)),
                Optional.of(new GeneratedContent("c-1", "script", "caption", "tiktok")),
                Optional.of(new VerificationResult("c-1", Verdict.APPROVE, List.of(), 0.95)),
                Optional.of(new PublishResult(
                        "c-1", "tiktok", PublishStatus.PUBLISHED,
                        Optional.of("post-1"), Optional.of(now), Optional.empty()
                )),
                Optional.empty()
        );

        String json = Json.toJson(result);
        PipelineResult restored = Json.fromJson(json, PipelineResult.class);

        assertEquals(result, restored);
        // Spot-check that Instant survived as ISO-8601, not epoch millis.
        assertTrue(json.contains("2026-04-19T12:34:56.789Z"),
                "Instant should serialize as ISO-8601: " + json);
    }

    @Test
    void roundTripEmptyResult() {
        var empty = new PipelineResult(
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.of("no trends")
        );

        String json = Json.toJson(empty);
        PipelineResult restored = Json.fromJson(json, PipelineResult.class);

        assertEquals(empty, restored);
    }
}
