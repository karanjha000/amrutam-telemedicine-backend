package com.amrutam.common.exception;

/**
 * Machine-readable error codes carried inside {@link RestResponse} envelopes.
 * Each constant declares a default HTTP status so transport layers can
 * translate codes without a second lookup table.
 */
public enum ApiErrorCode {

    NOT_FOUND(404),
    CONFLICT(409),
    FORBIDDEN(403),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    RATE_LIMITED(429),
    INTERNAL(500),
    IDEMPOTENCY_REPLAY_CONFLICT(409);

    private final int httpStatus;

    ApiErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
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
