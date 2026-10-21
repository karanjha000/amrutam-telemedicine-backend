package com.amrutam.api.repository;

import com.amrutam.api.entity.IdempotencyKey;
import com.amrutam.api.entity.IdempotencyState;
import com.amrutam.common.idempotency.IdempotencyRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link IdempotencyKey}.
 *
 * <p>Also implements the shared {@link IdempotencyRepository} SPI
 * (amrutam-common) via default methods, so the common
 * {@code DefaultIdempotencyService} can be wired against a single api bean.</p>
 */
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID>, IdempotencyRepository {

    /**
     * Look up a row in any of the given states.
     *
     * @param userId user
     * @param method http method
     * @param path request path
     * @param bodyHash body hash
     * @param states states to match
     * @return the most recent matching row
     */
    Optional<IdempotencyKey> findFirstByUserIdAndMethodAndPathAndBodyHashAndStateIn(
            UUID userId, String method, String path, String bodyHash, List<IdempotencyState> states);

    /**
     * Concrete insert of a PENDING row.
     *
     * <p>Spring Data requires modifying queries to return either {@code void} or
     * an integer count. Returning the inserted entity is invalid and triggers a
     * runtime error during booking execution.</p>
     *
     * @param id surrogate key
     * @param userId user
     * @param method http method
     * @param path request path
     * @param bodyHash body hash
     * @param now timestamp
     */
    @Modifying
    @Query("insert into IdempotencyKey(id, userId, method, path, bodyHash, state, resultJson, createdAt, updatedAt) "
            + "values (:id, :userId, :method, :path, :bodyHash, 'PENDING', null, :now, :now)")
    void insertRow(@Param("id") UUID id,
                   @Param("userId") UUID userId,
                   @Param("method") String method,
                   @Param("path") String path,
                   @Param("bodyHash") String bodyHash,
                   @Param("now") Instant now);

    /**
     * SPI: {@inheritDoc}
     */
    @Override
    @Transactional
    default void insertPending(UUID userId, String method, String path, String bodyHash) {
        insertRow(UUID.randomUUID(), userId, method, path, bodyHash, Instant.now());
    }

    /**
     * SPI: {@inheritDoc}
     */
    @Override
    default Optional<StoredKey> findActive(UUID userId, String method, String path, String bodyHash) {
        return findFirstByUserIdAndMethodAndPathAndBodyHashAndStateIn(
                        userId, method, path, bodyHash,
                        List.of(IdempotencyState.PENDING, IdempotencyState.COMPLETED, IdempotencyState.FAILED))
                .map(k -> new StoredKey(
                        k.getId(), k.getUserId(), k.getMethod(), k.getPath(),
                        k.getBodyHash(), k.getState().name(), k.getResultJson(),
                        k.getCreatedAt()));
    }

    /**
     * Concrete update to COMPLETED with the stored result.
     *
     * @param userId user
     * @param method http method
     * @param path request path
     * @param bodyHash body hash
     * @param resultJson serialized result
     * @param now timestamp
     * @return rows affected
     */
    @Modifying
    @Query("update IdempotencyKey k set k.state = 'COMPLETED', k.resultJson = :resultJson, "
            + "k.updatedAt = :now where k.userId = :userId and k.method = :method "
            + "and k.path = :path and k.bodyHash = :bodyHash")
    int completeRow(@Param("userId") UUID userId,
                    @Param("method") String method,
                    @Param("path") String path,
                    @Param("bodyHash") String bodyHash,
                    @Param("resultJson") String resultJson,
                    @Param("now") Instant now);

    /**
     * SPI: {@inheritDoc}
     */
    @Override
    @Transactional
    default void markCompleted(UUID userId, String method, String path, String bodyHash, String resultJson) {
        completeRow(userId, method, path, bodyHash, resultJson, Instant.now());
    }

    /**
     * Concrete update to FAILED.
     *
     * @param userId user
     * @param method http method
     * @param path request path
     * @param bodyHash body hash
     * @param now timestamp
     * @return rows affected
     */
    @Modifying
    @Query("update IdempotencyKey k set k.state = 'FAILED', k.updatedAt = :now "
            + "where k.userId = :userId and k.method = :method "
            + "and k.path = :path and k.bodyHash = :bodyHash")
    int failRow(@Param("userId") UUID userId,
                 @Param("method") String method,
                 @Param("path") String path,
                 @Param("bodyHash") String bodyHash,
                 @Param("now") Instant now);

    /**
     * SPI: {@inheritDoc}
     */
    @Override
    @Transactional
    default void markFailed(UUID userId, String method, String path, String bodyHash) {
        failRow(userId, method, path, bodyHash, Instant.now());
    }
}
