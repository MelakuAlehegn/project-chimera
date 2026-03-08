package com.chimera;

/**
 * Request for generating content from a trend and persona context.
 *
 * Includes a characterReferenceId field so that skills can maintain
 * character consistency, plus an explicit budget for the Resource Governor.
 */
public record ContentGenerationRequest(
        String topic,
        String characterReferenceId,
        double budget
) {
}

