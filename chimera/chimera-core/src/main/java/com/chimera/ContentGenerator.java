package com.chimera;

/**
 * Interface for the ContentGenerator skill.
 *
 * Implementations will turn a ContentGenerationRequest into a GeneratedContent
 * instance, potentially throwing a BudgetExceededException when Resource
 * Governor constraints are violated.
 */
public interface ContentGenerator {

    GeneratedContent generate(ContentGenerationRequest req) throws BudgetExceededException;
}

