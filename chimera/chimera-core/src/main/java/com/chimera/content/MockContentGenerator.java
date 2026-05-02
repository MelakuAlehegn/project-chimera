package com.chimera.content;

/**
 * Mock implementation of ContentGenerator for development and testing.
 *
 * Returns templated content built from the request's topic.
 * Enforces a minimum budget threshold to exercise BudgetExceededException.
 * No external API calls are made.
 */
public class MockContentGenerator implements ContentGenerator {

    private static final double MINIMUM_BUDGET = 1.00;

    @Override
    public GeneratedContent generate(ContentGenerationRequest req) throws BudgetExceededException {
        if (req.budget() < MINIMUM_BUDGET) {
            throw new BudgetExceededException(
                    "Budget %.2f is below the minimum %.2f required for content generation."
                            .formatted(req.budget(), MINIMUM_BUDGET));
        }

        return new GeneratedContent(
                "mock-" + req.topic().hashCode(),
                "Hook: Did you know about " + req.topic() + "?\n"
                        + "Main: Here's what you need to know...\n"
                        + "CTA: Follow for more tips!",
                "Discover " + req.topic() + " today",
                req.targetPlatform()
        );
    }
}
