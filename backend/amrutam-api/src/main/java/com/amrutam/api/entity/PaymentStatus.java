package com.amrutam.api.entity;

/**
 * Lifecycle states of a {@link Payment}.
 */
public enum PaymentStatus {
    /** Payment intent created. */ CREATED,
    /** Payment in progress at the gateway. */ PAYING,
    /** Gateway settled the payment. */ CAPTURED,
    /** Gateway failed / declined. */ FAILED,
    /** Refunded to the patient. */ REFUNDED
}
