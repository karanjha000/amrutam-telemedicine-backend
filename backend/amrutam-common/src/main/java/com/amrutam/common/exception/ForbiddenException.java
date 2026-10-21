package com.amrutam.common.exception;

/**
 * Raised when the authenticated principal lacks the required role or
 * permission; maps to HTTP 403.
 */
public class ForbiddenException extends AmrutamException {

    /**
     * Creates a new ForbiddenException.
     *
     * @param message the human-readable description of the missing permission
     */
    public ForbiddenException(String message) {
        super(message);
    }

    /**
     * Creates a new ForbiddenException with a cause.
     *
     * @param message the human-readable description of the missing permission
     * @param cause   the underlying cause
     */
    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
