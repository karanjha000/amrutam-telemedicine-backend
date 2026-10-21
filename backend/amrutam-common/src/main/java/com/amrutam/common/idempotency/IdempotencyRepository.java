package com.amrutam.common.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage SPI for idempotency keys. A plain interface so the common module
 * stays JPA-agnostic; the api module provides the JPA implementation.
 *
 * <p>The state constants live on the interface (rather than the record) so
 * that switch statements can be written against them directly.
 */
public interface IdempotencyRepository {

    /** The PENDING state value. */
    String PENDING = "PENDING";

    /** The COMPLETED state value. */
    String COMPLETED = "COMPLETED";

    /** The FAILED state value. */
    String FAILED = "FAILED";

    /**
     * Inserts a new PENDING row.
     *
     * @param userId   the user the key belongs to
     * @param method   HTTP method
     * @param path     request path
     * @param bodyHash SHA-256 hex of the request body (salted)
     */
    void insertPending(UUID userId, String method, String path, String bodyHash);

    /**
     * Finds the active (PENDING or COMPLETED) row for a key, if any.
     *
     * @param userId   the user the key belongs to
     * @param method   HTTP method
     * @param path     request path
     * @param bodyHash SHA-256 hex of the request body (salted)
     * @return the stored row when present
     */
    Optional<StoredKey> findActive(UUID userId, String method, String path, String bodyHash);

    /**
     * Marks a PENDING row COMPLETED with its serialized result.
     *
     * @param userId     the user the key belongs to
     * @param method     HTTP method
     * @param path       request path
     * @param bodyHash   the body hash
     * @param resultJson the serialized result payload
     */
    void markCompleted(UUID userId, String method, String path, String bodyHash, String resultJson);

    /**
     * Marks a PENDING row FAILED.
     *
     * @param userId   the user the key belongs to
     * @param method   HTTP method
     * @param path     request path
     * @param bodyHash the body hash
     */
    void markFailed(UUID userId, String method, String path, String bodyHash);

    /**
     * A persisted idempotency key row.
     *
     * @param keyId     primary key
     * @param userId    owner
     * @param method    HTTP method
     * @param path      request path
     * @param bodyHash  request body hash
     * @param state     PENDING, COMPLETED or FAILED
     * @param resultJson the stored result (only when COMPLETED)
     * @param createdAt row creation time
     */
    record StoredKey(
            UUID keyId,
            UUID userId,
            String method,
            String path,
            String bodyHash,
            String state,
            String resultJson,
            Instant createdAt) {
    }
}
