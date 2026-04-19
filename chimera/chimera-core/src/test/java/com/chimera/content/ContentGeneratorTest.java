package com.chimera.content;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract and behavior tests for the ContentGenerator skill.
 *
 * Structural tests use reflection to verify the type system matches specs/technical.md.
 * Behavior tests exercise MockContentGenerator to verify the contract is honored.
 */
class ContentGeneratorTest {

    // --- Structural contract (via reflection) ---

    @Test
    void contentGeneratorShouldBeAnInterfaceWithGenerateMethod() {
        Class<?> generatorInterface = ContentGenerator.class;

        assertTrue(generatorInterface.isInterface(), "ContentGenerator must be defined as an interface.");

        Method generate = Arrays.stream(generatorInterface.getMethods())
                .filter(m -> m.getName().equals("generate"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("ContentGenerator must declare a generate(...) method."));

        Class<?>[] parameterTypes = generate.getParameterTypes();
        assertEquals(1, parameterTypes.length, "generate(...) should accept a single request parameter.");
        assertEquals(ContentGenerationRequest.class, parameterTypes[0],
                "generate(...) should accept a ContentGenerationRequest.");

        assertEquals(GeneratedContent.class, generate.getReturnType(),
                "generate(...) should return a GeneratedContent result.");
    }

    @Test
    void contentGenerationRequestShouldIncludeCharacterConsistencyParameters() {
        Class<?> requestClass = ContentGenerationRequest.class;

        assertTrue(requestClass.isRecord(), "ContentGenerationRequest must be a Java 21 record.");

        var components = requestClass.getRecordComponents();
        var names = Arrays.stream(components).map(c -> c.getName()).toList();

        assertTrue(names.contains("characterReferenceId"),
                "ContentGenerationRequest must include a characterReferenceId component for character consistency.");
    }

    @Test
    void generateShouldDeclareBudgetExceededException() {
        Class<?> generatorInterface = ContentGenerator.class;

        Method generate = Arrays.stream(generatorInterface.getMethods())
                .filter(m -> m.getName().equals("generate"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("ContentGenerator must declare a generate(...) method."));

        Class<?>[] exceptionTypes = generate.getExceptionTypes();

        assertTrue(Arrays.asList(exceptionTypes).contains(BudgetExceededException.class),
                "generate(...) must declare BudgetExceededException to surface Resource Governor budget failures.");
    }

    @Test
    void generatedContentRecordShouldMatchOutputContract() {
        Class<?> resultClass = GeneratedContent.class;

        assertTrue(resultClass.isRecord(), "GeneratedContent must be a Java 21 record.");

        var components = resultClass.getRecordComponents();
        var names = Arrays.stream(components).map(c -> c.getName()).toList();

        assertTrue(names.contains("contentId"), "GeneratedContent must include contentId.");
        assertTrue(names.contains("script"), "GeneratedContent must include script.");
        assertTrue(names.contains("caption"), "GeneratedContent must include caption.");
        assertTrue(names.contains("targetPlatform"), "GeneratedContent must include targetPlatform.");
    }

    @Test
    void budgetExceededExceptionShouldBeACheckedException() {
        assertTrue(Exception.class.isAssignableFrom(BudgetExceededException.class),
                "BudgetExceededException must extend Exception (checked).");
        assertFalse(RuntimeException.class.isAssignableFrom(BudgetExceededException.class),
                "BudgetExceededException must NOT be a RuntimeException -- budget enforcement is a checked contract.");
    }

    // --- Behavior (via MockContentGenerator) ---

    @Test
    void generateShouldReturnContentMatchingRequestTopic() throws BudgetExceededException {
        ContentGenerator generator = new MockContentGenerator();
        var request = new ContentGenerationRequest("morning workout routine", "fit_chimera_v1", 5.00);

        GeneratedContent result = generator.generate(request);

        assertNotNull(result.contentId(), "contentId must not be null.");
        assertNotNull(result.script(), "script must not be null.");
        assertNotNull(result.caption(), "caption must not be null.");
        assertNotNull(result.targetPlatform(), "targetPlatform must not be null.");
        assertTrue(result.script().contains("morning workout routine"),
                "Script should reference the requested topic.");
    }

    @Test
    void generateShouldThrowBudgetExceededForInsufficientBudget() {
        ContentGenerator generator = new MockContentGenerator();
        var request = new ContentGenerationRequest("morning workout routine", "fit_chimera_v1", 0.50);

        assertThrows(BudgetExceededException.class, () -> generator.generate(request),
                "generate() must throw BudgetExceededException when budget is insufficient.");
    }
}
