package com.chimera;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests describing the expected interface for a ContentGenerator skill.
 *
 * These tests are written ahead of implementation and will fail to compile
 * or run until the ContentGenerator interface, request/response records,
 * and BudgetExceededException are created.
 */
class ContentGeneratorTest {

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
        // Ensure Character Consistency is explicitly modeled via a character_reference_id field
        Class<?> requestClass = ContentGenerationRequest.class;

        assertTrue(requestClass.isRecord(), "ContentGenerationRequest must be a Java 21 record.");

        var components = requestClass.getRecordComponents();
        var names = Arrays.stream(components).map(c -> c.getName()).toList();

        assertTrue(names.contains("characterReferenceId"),
                "ContentGenerationRequest must include a characterReferenceId component for character consistency.");
    }

    @Test
    void generateShouldSurfaceBudgetExceededExceptionFromResourceGovernor() {
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
        // Validate high-level shape of the GeneratedContent result used by agents
        Class<?> resultClass = GeneratedContent.class;

        assertTrue(resultClass.isRecord(), "GeneratedContent must be a Java 21 record.");

        var components = resultClass.getRecordComponents();
        var names = Arrays.stream(components).map(c -> c.getName()).toList();

        assertTrue(names.contains("contentId"), "GeneratedContent must include contentId.");
        assertTrue(names.contains("script"), "GeneratedContent must include script.");
        assertTrue(names.contains("caption"), "GeneratedContent must include caption.");
        assertTrue(names.contains("targetPlatform"), "GeneratedContent must include targetPlatform.");
    }
}

