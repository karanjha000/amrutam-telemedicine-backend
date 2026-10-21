package com.amrutam.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Read-side representation of a doctor.
 *
 * @param id doctor id
 * @param userId linked user id
 * @param name display name (falls back to username)
 * @param specialization specialization
 * @param consultationFee fee per consultation
 * @param yearsExperience years of experience
 * @param bio bio
 * @param avatarUrl avatar url
 * @param ratings average rating
 * @param totalRatings number of ratings
 * @param slots optional availability window (only populated when requested)
 */
public record DoctorDto(UUID id,
                       UUID userId,
                       String name,
                       String specialization,
                       BigDecimal consultationFee,
                       int yearsExperience,
                       String bio,
                       String avatarUrl,
                       double ratings,
                       int totalRatings,
                       List<AvailabilitySlotDto> slots) {
}
