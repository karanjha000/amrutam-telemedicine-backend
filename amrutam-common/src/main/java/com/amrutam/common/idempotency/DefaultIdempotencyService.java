package com.amrutam.common.idempotency;

import com.amrutam.common.exception.ConflictException;
import com.amrutam.common.idempotency.IdempotencyRepository;
import com.amrutam.common.idempotency.IdempotencyRepository.StoredKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Default IdempotencyService implementation backed by an
 * IdempotencyRepository.
 *
 * Semantics:
 * - A COMPLETED row immediately returns a replayed outcome.
 * - A PENDING row older than the pending timeout is considered expired:
 *   it is marked FAILED and the operation is re-run.
 * - Otherwise a fresh PENDING row is inserted; the operation is run, and
 *   the row is marked COMPLETED (with the serialized payload) or FAILED
 *   (and the exception is rethrown).
 * - A PENDING row that is still active (i.e. someone else is working)
 *   raises ConflictException - the client should retry later.
 */
public class DefaultIdempotencyService implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(DefaultIdempotencyService.class);

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;
    private final long pendingTimeoutMinutes;

    /**
     * Creates a service with the default 5-minute pending timeout.
     *
     * @param repository the storage
     * @param objectMapper the JSON mapper used to (de)serialize results
     */
    public DefaultIdempotencyService(IdempotencyRepository repository, ObjectMapper objectMapper) {
        this(repository, objectMapper, 5L);
    }

    /**
     * Creates a service with an explicit pending timeout.
     *
     * @param repository the storage
     * @param objectMapper the JSON mapper used to (de)serialize results
     * @param pendingTimeoutMinutes the PENDING row expiry
     */
    public DefaultIdempotencyService(
            IdempotencyRepository repository,
            ObjectMapper objectMapper,
            long pendingTimeoutMinutes) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.pendingTimeoutMinutes = pendingTimeoutMinutes;
    }

    @Override
    public IdempotencyOutcome apply(
            UUID userId, String method, String path, String bodyHash, Supplier<Object> operation) {

        var existing = repository.findActive(userId, method, path, bodyHash);
        if (existing.isPresent()) {
            StoredKey row = existing.get();
            switch (row.state()) {
                case IdempotencyRepository.COMPLETED:
                    return new ReplayedOutcome(row.resultJson());
                case IdempotencyRepository.PENDING:
                    if (isExpired(row)) {
                        log.info("Expiring stale PENDING key userId={} {} {}", userId, method, path);
                        repository.markFailed(userId, method, path, bodyHash);
                        return runFresh(userId, method, path, bodyHash, operation);
                    }
                    throw new ConflictException(
                            "Operation for this request is already in progress");
                default:
                    // FAILED rows fall through to a fresh execution.
                    return runFresh(userId, method, path, bodyHash, operation);
            }
        }

        repository.insertPending(userId, method, path, bodyHash);
        return runFresh(userId, method, path, bodyHash, operation);
    }

    private IdempotencyOutcome runFresh(
            UUID userId, String method, String path, String bodyHash, Supplier<Object> operation) {
        Object raw = operation.get();
        IdempotencyResult result = raw instanceof IdempotencyResult r ? r : IdempotencyResult.of(raw);
        String resultJson = serialize(result);
        repository.markCompleted(userId, method, path, bodyHash, resultJson);
        return new FreshOutcome(result, resultJson);
    }

    private String serialize(Object result) {
        try {
            Object payload =
                    result instanceof IdempotencyResult r ? r.payload() : result;
            if (payload == null) {
                return null;
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize idempotency payload: {}", e.getMessage());
            throw new ConflictException("Failed to persist idempotency result", e);
        }
    }

    private boolean isExpired(StoredKey row) {
        if (row.createdAt() == null) {
            return true;
        }
        return Duration.between(row.createdAt(), Instant.now()).toMinutes() >= pendingTimeoutMinutes;
    }

    /**
     * Outcome produced from a fresh execution.
     */
    public static final class FreshOutcome implements IdempotencyOutcome {
        private final IdempotencyResult result;
        private final String replayedPayload;

        FreshOutcome(IdempotencyResult result, String replayedPayload) {
            this.result = result;
            this.replayedPayload = replayedPayload;
        }

        @Override
        public IdempotencyResult result() {
            return result;
        }

        @Override
        public boolean replayed() {
            return false;
        }

        @Override
        public String replayedPayload() {
            return replayedPayload;
        }
    }

    /**
     * Outcome produced from a COMPLETED stored row.
     */
    public static final class ReplayedOutcome implements IdempotencyOutcome {
        private final String storedJson;

        ReplayedOutcome(String storedJson) {
            this.storedJson = storedJson;
        }

        @Override
        public IdempotencyResult result() {
            return new IdempotencyReplayResult(storedJson);
        }

        @Override
        public boolean replayed() {
            return true;
        }

        @Override
        public String replayedPayload() {
            return storedJson;
        }
    }
}
