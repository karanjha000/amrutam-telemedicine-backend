package com.amrutam.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for login.
 *
 * @param username username
 * @param password password
 * @param mfaCode optional TOTP code (required when MFA is enabled)
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        String mfaCode) {
}
