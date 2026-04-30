package com.chimera.publisher;

import java.time.Instant;
import java.util.Optional;

/**
 * Result of a publish attempt. Optional fields are absent when not applicable
 * (e.g., platformPostId is empty when status is FAILED).
 */
public record PublishResult(
        String contentId,
        String targetPlatform,
        PublishStatus status,
        Optional<String> platformPostId,
        Optional<Instant> publishedAt,
        Optional<String> error
) {
}
