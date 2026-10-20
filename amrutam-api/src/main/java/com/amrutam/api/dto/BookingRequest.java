package com.amrutam.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Payload for booking a consultation.
 *
 * @param doctorId doctor to book with
 * @param slotId slot to book
 * @param paymentMethod payment method hint (e.g. {@code card})
 * @param paymentToken gateway payment token (nullable in this scope; capture is
 *                     simulated)
 */
public record BookingRequest(
        @NotNull UUID doctorId,
        @NotNull UUID slotId,
        String paymentMethod,
        String paymentToken) {
}
