package com.amrutam.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for patient registration.
 *
 * @param username desired username (3–64 chars, unique)
 * @param password desired password (8–128 chars)
 * @param email email address
 * @param displayName human-readable name
 * @param phone optional phone number
 */
public record RegisterPatientRequest(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Email String email,
        @NotBlank @Size(max = 128) String displayName,
        @Size(max = 32) String phone) {
}
