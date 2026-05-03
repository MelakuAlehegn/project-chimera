package com.chimera.agent;

import com.chimera.content.GeneratedContent;
import com.chimera.trend.Trend;

/**
 * A single piece of work produced by a Worker.
 *
 * Pairs the trend the Worker chose with the content it generated for that
 * trend. The Judge receives a Candidate, evaluates it, and either approves,
 * sends it back for revision, or rejects it.
 */
public record Candidate(
        Trend selectedTrend,
        GeneratedContent content
) {
}
