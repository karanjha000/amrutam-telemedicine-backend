package com.amrutam.api.dto;

/**
 * MFA challenge returned from {@code /auth/login} when the user has TOTP
 * enabled.
 *
 * @param secret         base32 secret (only at provisioning time; subsequent
 *                       logins carry just the otpauth URI)
 * @param provisioningUri otpauth:// URI for the authenticator app
 */
public record MfaChallengeDto(String secret, String provisioningUri) {
}
