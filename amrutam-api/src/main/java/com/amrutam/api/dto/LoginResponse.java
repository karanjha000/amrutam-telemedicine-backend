package com.amrutam.api.dto;

/**
 * Result of {@code /auth/login}: either a bearer token (and the user) or an
 * MFA challenge when TOTP is enabled.
 *
 * @param token access token (null when an MFA challenge is returned)
 * @param mfaChallenge MFA challenge (null when a token is returned)
 * @param user the authenticated user
 */
public record LoginResponse(String token, MfaChallengeDto mfaChallenge, UserDto user) {
}
