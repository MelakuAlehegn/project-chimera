package com.chimera.persistence;

import com.chimera.orchestrator.PipelineRequest;
import com.chimera.orchestrator.PipelineResult;

import java.time.Instant;

/**
 * One pipeline execution captured as a single immutable event.
 * Together, the sequence of RunRecords IS the agent's memory.
 */
public record RunRecord(
        String runId,
        Instant runAt,
        PipelineRequest goal,
        PipelineResult result
) {
}
