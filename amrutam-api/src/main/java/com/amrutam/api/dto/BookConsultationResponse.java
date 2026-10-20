package com.amrutam.api.dto;

import java.util.UUID;

/**
 * Result of {@code BookingService.book}.
 *
 * @param consultationId created consultation
 * @param slotId booked slot
 * @param paymentId created payment
 * @param status final status string (typically {@code PAID})
 * @param replayed true when the result was served from an idempotency replay
 */
public record BookConsultationResponse(UUID consultationId,
                                      UUID slotId,
                                      UUID paymentId,
                                      String status,
                                      boolean replayed) {
}
