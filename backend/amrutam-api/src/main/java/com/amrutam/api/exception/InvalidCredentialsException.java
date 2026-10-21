package com.amrutam.api.exception;

import com.amrutam.common.exception.AmrutamException;

/**
 * Signals a failed credential check (unknown user or bad password);
 * maps to HTTP 401 via {@link GlobalExceptionHandler}.
 */
public class InvalidCredentialsException extends AmrutamException {

    /**
     * @param message human-readable message
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }

    /**
     * @param message human-readable message
     * @param cause underlying cause
     */
    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
