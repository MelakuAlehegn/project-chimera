package com.chimera.publisher;

import com.chimera.config.Config;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: posts a real message to your configured Bluesky account.
 *
 * Requires:
 *   - .env with BLUESKY_HANDLE and BLUESKY_APP_PASSWORD
 *   - Internet connectivity
 *
 * Side effect: creates a real public post you can see (and delete) on bsky.app.
 * The post text includes the run timestamp so duplicate runs are visibly distinct.
 */
@Tag("integration")
class BlueskyPlatformPublisherIntegrationTest {

    @Test
    void postsRealMessageToBluesky() {
        var publisher = new BlueskyPlatformPublisher(Config.load());
        var caption = "chimera-test " + Instant.now() + " -- ignore me";
        var request = new PublishRequest(
                "test-" + Instant.now().getEpochSecond(),
                "(unused script body)",
                caption,
                "bluesky"
        );

        PublishResult result = publisher.publish(request);

        if (result.status() == PublishStatus.FAILED) {
            fail("Publish failed: " + result.error().orElse("(no error)"));
        }

        assertEquals(PublishStatus.PUBLISHED, result.status());
        assertTrue(result.platformPostId().isPresent());
        assertTrue(result.publishedAt().isPresent());
        assertTrue(result.error().isEmpty());

        System.out.println("\n=== Bluesky post created ===");
        System.out.println("Post URI: " + result.platformPostId().orElse("(none)"));
        System.out.println("Published at: " + result.publishedAt().orElse(null));
    }
}
