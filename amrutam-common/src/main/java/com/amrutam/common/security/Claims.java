package com.amrutam.common.security;

import java.util.List;
import java.util.UUID;

/**
 * The verified claim set extracted from a platform-issued JWT.
 *
 * @param userId the user id encoded in the token
 * @param roles  the authority strings (e.g. {@code ROLE_PATIENT})
 * @param jwtId  the {@code jti} claim (idempotency/audit correlation)
 * @param iat    issued-at (epoch seconds)
 * @param exp    expiry (epoch seconds)
 */
public record Claims(UUID userId, List<String> roles, String jwtId, long iat, long exp) {
}
