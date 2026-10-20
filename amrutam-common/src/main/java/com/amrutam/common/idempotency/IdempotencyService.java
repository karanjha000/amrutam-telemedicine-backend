package com.amrutam.common.idempotency;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Idempotency SPI: applies the "run-once" semantic to a logical operation
 * keyed by (user, method, path, body hash).
 *
 * <p>The operation supplier produces the payload that will be serialized to
 * {@code result_json} and replayed on a subsequent call with the same key.
 * Callers may either pass a raw object wrapped via
 * {@link IdempotencyResult#of(Object)} or an {@link IdempotencyResult}
 * directly; both are accepted so the API stays friendly to non-marker
 * results.</p>
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
     * @param operation the operation to run when not replayed; its value
     *                  (typically an {@link IdempotencyResult} or any
     *                  JSON-serializable object) is persisted and replayed
     * @return the outcome (fresh or replayed)
     */
    IdempotencyOutcome apply(
            UUID userId,
            String method,
            String path,
            String bodyHash,
            Supplier<Object> operation);
}
