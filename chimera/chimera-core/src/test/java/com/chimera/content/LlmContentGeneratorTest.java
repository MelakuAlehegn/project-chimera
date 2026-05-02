package com.chimera.content;

import com.chimera.llm.LlmClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LlmContentGenerator using a stub LlmClient.
 *
 * No real API calls. We verify how the generator handles different LLM
 * outputs: clean JSON, code-fenced JSON, malformed output, missing fields.
 */
class LlmContentGeneratorTest {

    @Test
    void generatesContentFromCleanJsonResponse() throws BudgetExceededException {
        LlmClient stub = prompt -> """
                {"script": "Hello world script", "caption": "short caption"}
                """;
        var generator = new LlmContentGenerator(stub);
        var req = new ContentGenerationRequest("the topic", "persona-1", 5.00, "bluesky");

        GeneratedContent result = generator.generate(req);

        assertEquals("Hello world script", result.script());
        assertEquals("short caption", result.caption());
        assertEquals("bluesky", result.targetPlatform());
        assertNotNull(result.contentId());
        assertTrue(result.contentId().startsWith("llm-"));
    }

    @Test
    void toleratesJsonWrappedInCodeFences() throws BudgetExceededException {
        LlmClient stub = prompt -> """
                ```json
                {"script": "Hello", "caption": "Hi"}
                ```
                """;
        var generator = new LlmContentGenerator(stub);
        var req = new ContentGenerationRequest("topic", "persona-1", 5.00, "bluesky");

        GeneratedContent result = generator.generate(req);

        assertEquals("Hello", result.script());
        assertEquals("Hi", result.caption());
    }

    @Test
    void throwsBudgetExceededBelowMinimum() {
        LlmClient stub = prompt -> "should never be called";
        var generator = new LlmContentGenerator(stub);
        var req = new ContentGenerationRequest("topic", "persona-1", 0.10, "bluesky");

        assertThrows(BudgetExceededException.class, () -> generator.generate(req));
    }

    @Test
    void throwsOnMalformedJson() {
        LlmClient stub = prompt -> "this is not json at all";
        var generator = new LlmContentGenerator(stub);
        var req = new ContentGenerationRequest("topic", "persona-1", 5.00, "bluesky");

        assertThrows(IllegalStateException.class, () -> generator.generate(req));
    }

    @Test
    void throwsWhenScriptFieldIsMissing() {
        LlmClient stub = prompt -> """
                {"caption": "only a caption"}
                """;
        var generator = new LlmContentGenerator(stub);
        var req = new ContentGenerationRequest("topic", "persona-1", 5.00, "bluesky");

        assertThrows(IllegalStateException.class, () -> generator.generate(req));
    }
}
