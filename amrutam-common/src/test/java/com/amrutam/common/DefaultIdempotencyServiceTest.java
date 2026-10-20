package com.amrutam.common;

import com.amrutam.common.exception.ConflictException;
import com.amrutam.common.idempotency.DefaultIdempotencyService;
import com.amrutam.common.idempotency.IdempotencyOutcome;
import com.amrutam.common.idempotency.IdempotencyRepository;
import com.amrutam.common.idempotency.IdempotencyRepository.StoredKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultIdempotencyServiceTest {

    private final UUID userId = UUID.randomUUID();
    private final Map<String, StoredKey> store = new ConcurrentHashMap<>();

    private InMemoryRepo repo;
    private DefaultIdempotencyService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        store.clear();
        repo = new InMemoryRepo(store);
        service = new DefaultIdempotencyService(repo, mapper, 5L);
    }

    private static String keyOf(UUID userId, String method, String path, String hash) {
        return userId + "|" + method + "|" + path + "|" + hash;
    }

    private static final class InMemoryRepo implements IdempotencyRepository {
        private final Map<String, StoredKey> store;

        InMemoryRepo(Map<String, StoredKey> store) {
            this.store = store;
        }

        @Override
        public void insertPending(UUID userId, String method, String path, String bodyHash) {
            store.put(keyOf(userId, method, path, bodyHash),
                    new StoredKey(UUID.randomUUID(), userId, method, path, bodyHash,
                            IdempotencyRepository.PENDING, null, Instant.now()));
        }

        @Override
        public Optional<StoredKey> findActive(UUID userId, String method, String path, String bodyHash) {
            return Optional.ofNullable(store.get(keyOf(userId, method, path, bodyHash)));
        }

        @Override
        public void markCompleted(UUID userId, String method, String path, String bodyHash, String resultJson) {
            replace(userId, method, path, bodyHash, IdempotencyRepository.COMPLETED, resultJson);
        }

        @Override
        public void markFailed(UUID userId, String method, String path, String bodyHash) {
            replace(userId, method, path, bodyHash, IdempotencyRepository.FAILED, null);
        }

        private void replace(UUID userId, String method, String path, String bodyHash,
                            String newState, String resultJson) {
            StoredKey existing = store.get(keyOf(userId, method, path, bodyHash));
            if (existing != null) {
                store.put(keyOf(userId, method, path, bodyHash),
                        new StoredKey(existing.keyId(), userId, method, path, bodyHash,
                                newState, resultJson, existing.createdAt()));
            }
        }
    }

    record UserPayload(UUID id, String name) {
    }

    @Test
    void firstCallRunsOperationAndPersistsResult() {
        IdempotencyOutcome outcome = service.apply(userId, "POST", "/api/v1/appointments", "hash1",
                () -> new UserPayload(UUID.randomUUID(), "alice"));

        assertThat(outcome.replayed()).isFalse();
        StoredKey stored =
                repo.findActive(userId, "POST", "/api/v1/appointments", "hash1").orElseThrow();
        assertThat(stored.state()).isEqualTo(IdempotencyRepository.COMPLETED);
        assertThat(stored.resultJson()).contains("\"name\":\"alice\"");
    }

    @Test
    void secondCallReplaysStoredPayloadWithoutRerunningOperation() {
        int[] calls = {0};

        IdempotencyOutcome first = service.apply(userId, "POST", "/api/v1/appointments", "hash1",
                () -> new UserPayload(UUID.randomUUID(), "alice"));
        assertThat(first.replayed()).isFalse();

        IdempotencyOutcome replayed = service.apply(userId, "POST", "/api/v1/appointments", "hash1",
                () -> {
                    calls[0]++;
                    return new UserPayload(UUID.randomUUID(), "bob");
                });

        assertThat(replayed.replayed()).isTrue();
        assertThat(calls[0]).isZero();
        assertThat(replayed.replayedPayload()).contains("\"name\":\"alice\"");
    }

    @Test
    void activePendingRowThrowsConflict() {
        store.put(keyOf(userId, "POST", "/x", "h"), new StoredKey(
                UUID.randomUUID(), userId, "POST", "/x", "h",
                IdempotencyRepository.PENDING, null, Instant.now()));

        int[] calls = {0};
        assertThatThrownBy(() -> service.apply(userId, "POST", "/x", "h",
                        () -> {
                            calls[0]++;
                            return new UserPayload(UUID.randomUUID(), "z");
                        }))
                .isInstanceOf(ConflictException.class);
        assertThat(calls[0]).isZero();
    }

    @Test
    void expiredPendingRowIsExpiredAndOperationReruns() {
        store.put(keyOf(userId, "POST", "/y", "h2"), new StoredKey(
                UUID.randomUUID(), userId, "POST", "/y", "h2",
                IdempotencyRepository.PENDING, null,
                Instant.now().minus(java.time.Duration.ofMinutes(6))));

        IdempotencyOutcome outcome = service.apply(userId, "POST", "/y", "h2",
                () -> new UserPayload(UUID.randomUUID(), "renamed"));

        assertThat(outcome.replayed()).isFalse();
        StoredKey stored =
                repo.findActive(userId, "POST", "/y", "h2").orElseThrow();
        assertThat(stored.state()).isEqualTo(IdempotencyRepository.COMPLETED);
    }
}
