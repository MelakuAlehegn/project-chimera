package com.chimera.publisher;

import java.time.Instant;
import java.util.Optional;

/**
 * Mock implementation of PlatformPublisher for development and testing.
 *
 * Empty/null script -> FAILED. Otherwise -> PUBLISHED with a fake post ID.
 * No external API calls are made.
 */
public class MockPlatformPublisher implements PlatformPublisher {

    @Override
    public PublishResult publish(PublishRequest request) {
        if (request.script() == null || request.script().isBlank()) {
            return new PublishResult(
                    request.contentId(),
                    request.targetPlatform(),
                    PublishStatus.FAILED,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of("script is empty")
            );
        }

        return new PublishResult(
                request.contentId(),
                request.targetPlatform(),
                PublishStatus.PUBLISHED,
                Optional.of("mock-post-" + request.contentId()),
                Optional.of(Instant.now()),
                Optional.empty()
        );
    }
}
