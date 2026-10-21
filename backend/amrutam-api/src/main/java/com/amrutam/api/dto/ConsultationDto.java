package com.amrutam.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-side representation of a consultation.
 *
 * @param id consultation id
 * @param patientId patient
 * @param doctorId doctor
 * @param slotId slot
 * @param status status string
 * @param scheduledAt scheduled time
 * @param completedAt completion time (nullable)
 */
public record ConsultationDto(UUID id, UUID patientId, UUID doctorId, UUID slotId,
                              String status, Instant scheduledAt, Instant completedAt) {
}
