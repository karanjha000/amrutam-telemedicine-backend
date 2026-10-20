package com.amrutam.common.exception;

/**
 * Machine-readable error codes carried inside {@link RestResponse} envelopes.
 * Each constant declares a default HTTP status so transport layers can
 * translate codes without a second lookup table.
 */
public enum ApiErrorCode {

    NOT_FOUND("NOT_FOUND", 404),
    CONFLICT("CONFLICT", 409),
    FORBIDDEN("FORBIDDEN", 403),
    BAD_REQUEST("BAD_REQUEST", 400),
    UNAUTHORIZED("UNAUTHORIZED", 401),
    RATE_LIMITED("RATE_LIMITED", 429),
    INTERNAL("INTERNAL", 500),
    IDEMPOTENCY_REPLAY_CONFLICT("IDEMPOTENCY_REPLAY_CONFLICT", 409);

    private final String name;
    private final int httpStatus;

    ApiErrorCode(String name, int httpStatus) {
        this.name = name;
        this.httpStatus = httpStatus;
    }

    /**
     * The wire name of this code.
     *
     * @return code name
     */
    public String name() {
        return name;
    }

    /**
     * The default HTTP status associated with this code.
     *
     * @return HTTP status code
     */
    public int toHttpStatus() {
        return httpStatus;
    }
}
