package com.amrutam.common.idempotency;

/**
 * The result of an {@link IdempotencyService#apply} call: either a freshly
 * executed result or a replay of a previously stored one.
 */
public interface IdempotencyOutcome {

    /**
     * The result to return to the caller.
     *
     * @return the result
     */
    IdempotencyResult result();

    /**
     * Whether this outcome was produced from a stored replay rather than a
     * fresh execution.
     *
     * @return true when replayed
     */
    boolean replayed();

    /**
     * The stored JSON payload this outcome is based on (null for fresh
     * executions where the caller still holds the live result).
     *
     * @return the stored JSON or null
     */
    String replayedPayload();
}
