package com.amrutam.common.idempotency;

/**
 * Header names used for idempotent requests.
 */
public final class IdempotencyHeaders {

    /** The header carrying the client-generated idempotency key. */
    public static final String KEY = "X-Idempotency-Key";

    private IdempotencyHeaders() {
    }
}
