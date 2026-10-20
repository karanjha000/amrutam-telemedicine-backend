package com.amrutam.common.exception;

/**
 * Raised when the request payload or parameters are malformed;
 * maps to HTTP 400.
 */
public class BadRequestException extends AmrutamException {

    /**
     * Creates a new BadRequestException.
     *
     * @param message the human-readable validation failure description
     */
    public BadRequestException(String message) {
        super(message);
    }

    /**
     * Creates a new BadRequestException with a cause.
     *
     * @param message the human-readable validation failure description
     * @param cause   the underlying cause
     */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
