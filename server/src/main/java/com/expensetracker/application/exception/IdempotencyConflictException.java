package com.expensetracker.application.exception;

/**
 * Thrown when an idempotency key is reused with a different request payload.
 * Maps to 409 Conflict in the HTTP layer.
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
