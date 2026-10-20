package com.amrutam.common.exception;

/**
 * Base class for all Amrutam domain/transport exceptions.
 * <p>
 * The api layer maps this hierarchy onto {@link RestResponse} envelopes and
 * HTTP status codes; everything in common throws concrete subtypes of this
 * class rather than raw {@link RuntimeException}s.
 */
public class AmrutamException extends RuntimeException {

    /**
     * Creates a new exception with the given message.
     *
     * @param message the human-readable message
     */
    protected AmrutamException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the given message and cause.
     *
     * @param message the human-readable message
     * @param cause   the underlying cause
     */
    protected AmrutamException(String message, Throwable cause) {
        super(message, cause);
    }
}
