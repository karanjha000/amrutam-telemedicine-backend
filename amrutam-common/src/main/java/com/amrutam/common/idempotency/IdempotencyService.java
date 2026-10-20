package com.amrutam.common.idempotency;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Idempotency SPI: applies the "run-once" semantic to a logical operation
 * keyed by (user, method, path, body hash).
 */
public interface IdempotencyService {

    /**
     * Executes the operation exactly once for a given key; subsequent calls
     * with the same key replay the stored result.
     *
     * @param userId   the acting user
     * @param method   HTTP method (e.g. POST)
     * @param path     request path
     * @param bodyHash the salted SHA-256 hex of the request body
     * @param operation the operation to run when not replayed
     * @param <Result>  the result type (must implement {@link IdempotencyResult})
     * @return the outcome (fresh or replayed)
     */
    <Result extends IdempotencyResult> IdempotencyOutcome apply(
            UUID userId,
            String method,
            String path,
            String bodyHash,
            Supplier<Result> operation);
}
