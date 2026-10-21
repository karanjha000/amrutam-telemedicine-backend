package com.amrutam.common.idempotency;

/**
 * Marker interface for the result of an idempotent operation. Implementations
 * wrap the actual payload; the {@link com.amrutam.common.idempotency.IdempotencyService}
 * is responsible for serializing/deserializing the payload to/from its
 * storage when a replay occurs.
 */
public interface IdempotencyResult {

    /**
     * The raw payload.
     *
     * @return the payload object
     */
    Object payload();

    /**
     * Wraps an arbitrary object as an {@link IdempotencyResult}.
     *
     * @param payload the payload
     * @return a wrapping instance
     */
    static IdempotencyResult of(Object payload) {
        return () -> payload;
    }
}
