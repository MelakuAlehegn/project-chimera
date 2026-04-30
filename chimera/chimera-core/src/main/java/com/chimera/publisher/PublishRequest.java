package com.chimera.publisher;

/**
 * Request to publish content to a target platform.
 */
public record PublishRequest(
        String contentId,
        String script,
        String caption,
        String targetPlatform
) {
}
