package com.chimera.publisher;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatformPublisherTest {

    @Test
    void platformPublisherShouldBeAnInterface() {
        assertTrue(PlatformPublisher.class.isInterface(),
                "PlatformPublisher must be an interface.");
    }

    @Test
    void publishStatusShouldHaveLifecycleValues() {
        assertNotNull(PublishStatus.valueOf("SCHEDULED"));
        assertNotNull(PublishStatus.valueOf("PUBLISHED"));
        assertNotNull(PublishStatus.valueOf("FAILED"));
    }

    @Test
    void publishShouldSucceedForValidContent() {
        PlatformPublisher publisher = new MockPlatformPublisher();
        var request = new PublishRequest("draft-1", "Some script body", "Caption", "tiktok");

        PublishResult result = publisher.publish(request);

        assertEquals(PublishStatus.PUBLISHED, result.status());
        assertEquals("draft-1", result.contentId());
        assertTrue(result.platformPostId().isPresent(), "Successful publish should have a post ID.");
        assertTrue(result.publishedAt().isPresent(), "Successful publish should have a timestamp.");
        assertTrue(result.error().isEmpty(), "Successful publish should have no error.");
    }

    @Test
    void publishShouldFailForEmptyScript() {
        PlatformPublisher publisher = new MockPlatformPublisher();
        var request = new PublishRequest("draft-2", "", "Caption", "tiktok");

        PublishResult result = publisher.publish(request);

        assertEquals(PublishStatus.FAILED, result.status());
        assertTrue(result.platformPostId().isEmpty(), "Failed publish should have no post ID.");
        assertTrue(result.error().isPresent(), "Failed publish should have an error message.");
    }
}
