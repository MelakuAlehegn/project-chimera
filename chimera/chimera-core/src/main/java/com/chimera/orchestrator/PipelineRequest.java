package com.chimera.orchestrator;

/**
 * The high-level goal passed to ContentPipeline.
 *
 * Combines the inputs needed by every skill in the pipeline:
 *   - platform/category drive trend fetching
 *   - characterReferenceId/budget drive content generation
 *
 * The pipeline is responsible for translating this single goal into the
 * per-skill request shapes (TrendRequest, ContentGenerationRequest, etc.).
 */
public record PipelineRequest(
        String platform,
        String category,
        String characterReferenceId,
        double budget
) {
}
