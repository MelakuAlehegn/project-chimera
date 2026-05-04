package com.chimera.content;

import com.chimera.config.Config;
import com.chimera.llm.GeminiLlmClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test against the real Gemini API.
 *
 * Requires .env with a working GEMINI_API_KEY. Slow (~2-5s) and costs
 * free-tier quota. Verifies that the prompt + response shape work
 * end-to-end with a real LLM.
 */
@Tag("integration")
class LlmContentGeneratorIntegrationTest {

    @Test
    void generatesRealContentFromGemini() throws BudgetExceededException {
        var generator = new LlmContentGenerator(new GeminiLlmClient(Config.load()));
        var req = new ContentGenerationRequest(
                "morning workout routine", "fit_chimera_v1", 5.00, "bluesky"
        );

        GeneratedContent result = generator.generate(req);

        assertNotNull(result.script());
        assertNotNull(result.caption());
        assertFalse(result.script().isBlank(), "Script should not be blank.");
        assertFalse(result.caption().isBlank(), "Caption should not be blank.");
        assertEquals("bluesky", result.targetPlatform());

        // Print so the developer can read what the agent actually produced.
        // Visible only when running tests with surefire output enabled.
        System.out.println("\n=== Generated content ===");
        System.out.println("Script: " + result.script());
        System.out.println("Caption: " + result.caption());
    }
}
