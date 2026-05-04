package com.chimera.llm;

import com.chimera.config.Config;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that calls the real Gemini API.
 *
 * Requires:
 *   - .env with a valid GEMINI_API_KEY
 *   - Internet connectivity
 *
 * This test costs real (free-tier) API quota and is slow (~1-3 seconds).
 * Skipped automatically on CI environments where the API key isn't set.
 */
@Tag("integration")
class GeminiLlmClientIntegrationTest {

    @Test
    void geminiShouldReturnSomethingForASimplePrompt() {
        Config config = Config.load();
        LlmClient client = new GeminiLlmClient(config);

        String response = client.complete(
                "Reply with exactly one word: hello"
        );

        assertNotNull(response);
        assertFalse(response.isBlank(), "Gemini should return non-empty text.");
        // Loose assertion -- the model is non-deterministic. We just verify
        // that the round-trip works and we get text back.
    }
}
