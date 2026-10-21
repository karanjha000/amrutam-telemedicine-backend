package com.amrutam.common.security;

import com.amrutam.common.exception.AmrutamException;

/**
 * Raised when a JWT fails parsing or validation; the api layer maps it to
 * HTTP 401.
 */
public class JwtException extends AmrutamException {

    /**
     * Creates a generic JWT failure.
     *
     * @param message the description of the failure
     */
    protected JwtException(String message) {
        super(message);
    }

    /**
     * Creates a JWT failure with a cause.
     *
     * @param message the description of the failure
     * @param cause   the underlying cause
     */
    protected JwtException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Factory for the generic "unauthorized" flavour.
     *
     * @param msg the message
     * @return a new JwtException
     */
    public static JwtException unauthorized(String msg) {
        return new JwtException(msg);
    }

    /**
     * Factory for the "token expired" flavour.
     *
     * @param msg the message
     * @return a new JwtException
     */
    public static JwtException expired(String msg) {
        return new JwtException(msg);
    }
}
