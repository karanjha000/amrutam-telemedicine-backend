package com.amrutam.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Admin payload for creating a doctor account.
 *
 * @param username doctor username
 * @param password initial password
 * @param displayName display name
 * @param email email
 * @param specialization specialization
 * @param consultationFee fee
 * @param yearsExperience years of experience
 * @param bio bio
 */
public record DoctorCreateRequest(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank String displayName,
        @Size(max = 160) String email,
        @NotBlank String specialization,
        BigDecimal consultationFee,
        Integer yearsExperience,
        @Size(max = 2000) String bio) {
}
