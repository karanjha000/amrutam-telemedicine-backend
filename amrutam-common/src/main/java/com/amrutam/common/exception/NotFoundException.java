package com.amrutam.common.exception;

/**
 * Raised when a requested resource does not exist; maps to HTTP 404.
 */
public class NotFoundException extends AmrutamException {

    /**
     * Creates a new NotFoundException.
     *
     * @param resource the name of the resource type (e.g. "Appointment")
     * @param id       the missing identifier (may be an id string, email, etc.)
     */
    public NotFoundException(String resource, String id) {
        super(resource + " not found: " + id);
    }

    /**
     * Creates a new NotFoundException with a cause.
     *
     * @param resource the name of the resource type
     * @param id       the missing identifier
     * @param cause    the underlying cause
     */
    public NotFoundException(String resource, String id, Throwable cause) {
        super(resource + " not found: " + id, cause);
    }
}
