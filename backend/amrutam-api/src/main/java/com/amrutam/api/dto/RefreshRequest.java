package com.amrutam.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for {@code /auth/refresh}.
 *
 * @param token current (still valid) access token
 */
public record RefreshRequest(@NotBlank String token) {
}
