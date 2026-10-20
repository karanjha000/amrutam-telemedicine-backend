package com.amrutam.api.entity;

/**
 * Lifecycle states of a {@link Consultation}.
 */
public enum ConsultationStatus {
    /** Created, payment not yet settled. */ PENDING_PAYMENT,
    /** Payment captured. */ PAID,
    /** Consultation delivered. */ COMPLETED,
    /** Cancelled (patient/admin, before capture). */ CANCELLED,
    /** No-show (time passed, never completed). */ NO_SHOW
}
