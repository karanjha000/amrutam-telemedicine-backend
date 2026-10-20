package com.amrutam.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Admin / doctor-owner payload for creating an availability slot.
 *
 * @param startAt window start (inclusive)
 * @param endAt window end (exclusive)
 */
public record SlotCreateRequest(@NotNull Instant startAt, @NotNull Instant endAt) {
}
