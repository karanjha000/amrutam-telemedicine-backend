package com.amrutam.api.exception;

import com.amrutam.common.exception.AmrutamException;

/**
 * Raised when authentication credentials are missing, malformed, or
 * rejected by the account (unknown user, wrong password, disabled
 * account, MFA not satisfied).
 *
 * <p>Mapped to HTTP 401 by {@link com.amrutam.api.exception.GlobalExceptionHandler}.</p>
 */
public class InvalidCredentialsException extends AmrutamException {

    /**
     * Creates the exception.
     *
     * @param message the human-readable message
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
