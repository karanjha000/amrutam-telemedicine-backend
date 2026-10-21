package com.amrutam.common.exception;

/**
 * Raised when an operation conflicts with the current state
 * (duplicate slug, idempotency replay conflict, saga compensation clash...);
 * maps to HTTP 409.
 */
public class ConflictException extends AmrutamException {

    /**
     * Creates a new ConflictException.
     *
     * @param message the human-readable conflict description
     */
    public ConflictException(String message) {
        super(message);
    }

    /**
     * Creates a new ConflictException with a cause.
     *
     * @param message the human-readable conflict description
     * @param cause   the underlying cause
     */
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
