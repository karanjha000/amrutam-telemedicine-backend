package com.amrutam.api.entity;

/**
 * Lifecycle states of an {@link AvailabilitySlot}.
 */
public enum SlotStatus {
    /** Open for booking. */ AVAILABLE,
    /** Soft-locked by a patient mid-checkout. */ LOCKED,
    /** Booked into a consultation. */ BOOKED,
    /** Cancelled / released. */ CANCELLED
}
