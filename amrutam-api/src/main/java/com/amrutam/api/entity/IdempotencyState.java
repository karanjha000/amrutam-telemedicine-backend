package com.amrutam.api.entity;

/**
 * States of an idempotency key row.
 */
public enum IdempotencyState {
    /** Key registered, operation in flight. */ PENDING,
    /** Operation completed, result stored. */ COMPLETED,
    /** Operation failed. */ FAILED
}
