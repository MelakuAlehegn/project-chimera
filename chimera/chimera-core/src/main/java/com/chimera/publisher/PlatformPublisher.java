package com.chimera.publisher;

/**
 * Skill interface for publishing approved content to a social media platform.
 *
 * Implementations are responsible for idempotency: calling publish() twice with
 * the same contentId should not produce two posts on the target platform.
 */
public interface PlatformPublisher {

    PublishResult publish(PublishRequest request);
}
