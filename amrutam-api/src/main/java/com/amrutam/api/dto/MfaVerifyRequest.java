package com.amrutam.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Payload for {@code /auth/mfa/verify}.
 *
 * @param username username
 * @param code 6-digit TOTP code
 */
public record MfaVerifyRequest(
        @NotBlank String username,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "code must be 6 digits") String code) {
}
