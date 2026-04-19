package com.chimera.content;

/**
 * Exception thrown when the Resource Governor determines that
 * a content generation request would exceed the allowed budget.
 */
public class BudgetExceededException extends Exception {

    public BudgetExceededException() {
        super();
    }

    public BudgetExceededException(String message) {
        super(message);
    }

    public BudgetExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
