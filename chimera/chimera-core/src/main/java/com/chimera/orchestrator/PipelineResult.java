package com.chimera.orchestrator;

import com.chimera.content.GeneratedContent;
import com.chimera.publisher.PublishResult;
import com.chimera.trend.Trend;
import com.chimera.verifier.VerificationResult;

import java.util.Optional;

/**
 * Trace of a single pipeline run. Each field captures what one step produced,
 * or is empty if the pipeline stopped before reaching that step.
 *
 * This is the "flight recorder" -- humans and downstream agents inspect this to
 * understand why a run ended where it did.
 */
public record PipelineResult(
        Optional<Trend> selectedTrend,
        Optional<GeneratedContent> generatedContent,
        Optional<VerificationResult> verificationResult,
        Optional<PublishResult> publishResult,
        Optional<String> stoppedReason
) {
}
