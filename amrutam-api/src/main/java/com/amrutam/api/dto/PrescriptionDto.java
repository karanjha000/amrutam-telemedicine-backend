package com.amrutam.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-side representation of a prescription.
 *
 * @param id prescription id
 * @param consultationId consultation
 * @param patientId patient
 * @param doctorId doctor
 * @param medications medications (JSON text)
 * @param dosageNotes dosage notes
 */
public record PrescriptionDto(UUID id, UUID consultationId, UUID patientId, UUID doctorId,
                              String medications, String dosageNotes, Instant createdAt) {
}
