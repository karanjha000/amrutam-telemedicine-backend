package com.amrutam.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-side representation of an availability slot.
 *
 * @param id slot id
 * @param doctorId owning doctor
 * @param startAt window start
 * @param endAt window end
 * @param status slot status string
 */
public record AvailabilitySlotDto(UUID id, UUID doctorId, Instant startAt, Instant endAt, String status) {
}
