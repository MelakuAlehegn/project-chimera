package com.chimera.content;

/**
 * Result of a content generation call.
 *
 * Fields are aligned with the expectations in ContentGeneratorTest.
 */
public record GeneratedContent(
        String contentId,
        String script,
        String caption,
        String targetPlatform
) {
}
