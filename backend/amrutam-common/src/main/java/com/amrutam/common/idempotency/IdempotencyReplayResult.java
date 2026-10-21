package com.amrutam.common.idempotency;

/**
 * An IdempotencyResult whose payload is the stored JSON string from a
 * replay. Consumers who need the deserialized value can use an
 * ObjectMapper on the returned payload themselves; this keeps the common
 * module free of a result-type-erasure problem.
 */
public class IdempotencyReplayResult implements IdempotencyResult {

    private final String storedJson;

    /**
     * Creates the replay result.
     *
     * @param storedJson the stored JSON payload
     */
    public IdempotencyReplayResult(String storedJson) {
        this.storedJson = storedJson;
    }

    @Override
    public Object payload() {
        return storedJson;
    }
}
