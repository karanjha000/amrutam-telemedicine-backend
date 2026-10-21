package com.amrutam.common.idempotency;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Fallback idempotency service used when no repository-backed implementation is
 * present in the runtime. It executes the operation every time without storing
 * or replaying state.
 */
public class NoopIdempotencyService implements IdempotencyService {

    @Override
    public IdempotencyOutcome apply(UUID userId,
                                   String method,
                                   String path,
                                   String bodyHash,
                                   Supplier<Object> operation) {
        Object payload = operation.get();
        return new IdempotencyOutcome() {
            @Override
            public IdempotencyResult result() {
                return IdempotencyResult.of(payload);
            }

            @Override
            public boolean replayed() {
                return false;
            }

            @Override
            public String replayedPayload() {
                return null;
            }
        };
    }
}
