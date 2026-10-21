package com.amrutam.common.security;

import com.amrutam.common.exception.AmrutamException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Platform JWT implementation backed by com.auth0/java-jwt
 * (HMAC-SHA256 signature).
 */
public class JwtTokenService implements JwTokenService {

    /** Claim key for the user id. */
    public static final String CLAIM_USER_ID = "uid";
    /** Claim key for the roles list. */
    public static final String CLAIM_ROLES = "roles";

    private final Algorithm algorithm;
    private final Algorithm legacyAlgorithm;
    private final String issuer;
    private final int clockSkewSeconds;
    private final Clock clock;

    /**
     * Creates a service using the system UTC clock.
     *
     * @param props the configured properties
     */
    public JwtTokenService(JwtProperties props) {
        this(props, Clock.systemUTC());
    }

    /**
     * Creates a service with an injectable clock (for tests).
     *
     * @param props the configured properties
     * @param clock the clock used for issue/expiry evaluation
     */
    public JwtTokenService(JwtProperties props, Clock clock) {
        this.algorithm = Algorithm.HMAC256(normalizeKey(props.getSecret()));
        this.legacyAlgorithm = props.getLegacySecret() == null || props.getLegacySecret().isBlank()
                ? null
                : Algorithm.HMAC256(normalizeKey(props.getLegacySecret()));
        this.issuer = props.getIssuer();
        this.clockSkewSeconds = props.getClockSkewSeconds();
        this.clock = clock;
    }

    /**
     * Normalizes the configured secret to exactly 32 bytes so the derived
     * key length is explicit; short secrets are zero-padded, long ones
     * truncated.
     *
     * @param secret the raw secret
     * @return a normalized 32-char ASCII key
     */
    private static String normalizeKey(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        byte[] normalized = new byte[32];
        System.arraycopy(bytes, 0, normalized, 0, Math.min(bytes.length, 32));
        return new String(normalized, StandardCharsets.US_ASCII);
    }

    @Override
    public String issue(Subject subject, String jti, long ttlSeconds) {
        long now = clock.millis() / 1000L;
        com.auth0.jwt.JWTCreator.Builder builder = JWT.create()
                .withIssuer(issuer)
                .withSubject(subject.username())
                .withClaim(CLAIM_USER_ID, subject.userId().toString())
                .withClaim(CLAIM_ROLES, subject.roles())
                .withIssuedAt(new Date(now * 1000L))
                .withExpiresAt(new Date((now + ttlSeconds) * 1000L));
        if (jti != null) {
            builder.withJWTId(jti);
        }
        return builder.sign(algorithm);
    }

    @Override
    public JwTokenService.Claims parseAndValidate(String token) {
        try {
            return verifyWith(algorithm, token);
        } catch (JwtException primaryFailure) {
            if (legacyAlgorithm == null) {
                throw primaryFailure;
            }
            try {
                return verifyWith(legacyAlgorithm, token);
            } catch (JwtException ignored) {
                throw primaryFailure;
            }
        }
    }

    private JwTokenService.Claims verifyWith(Algorithm signingAlgorithm, String token) {
        JWTVerifier verifier = JWT.require(signingAlgorithm).withIssuer(issuer).build();
        DecodedJWT jwt;
        try {
            jwt = verifier.verify(token);
        } catch (TokenExpiredException e) {
            long nowSec = clock.millis() / 1000L;
            DecodedJWT decoded = JWT.decode(token);
            long expSec =
                    decoded.getExpiresAt() == null ? 0L : decoded.getExpiresAt().getTime() / 1000L;
            if (expSec + clockSkewSeconds >= nowSec) {
                jwt = decoded;
            } else {
                throw JwtException.expired("Token expired: " + e.getMessage());
            }
        } catch (JWTVerificationException e) {
            throw JwtException.unauthorized("Invalid token: " + e.getMessage());
        }
        return toClaims(jwt);
    }

    @Override
    public boolean canParse(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt != null && jwt.getPayload() != null;
        } catch (AmrutamException | IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    private JwTokenService.Claims toClaims(DecodedJWT jwt) {
        UUID userId = UUID.fromString(jwt.getClaim(CLAIM_USER_ID).asString());
        List<String> roles =
                jwt.getClaim(CLAIM_ROLES).asList(String.class) == null
                        ? List.of()
                        : List.copyOf(jwt.getClaim(CLAIM_ROLES).asList(String.class));
        return new JwTokenService.Claims(
                userId,
                roles,
                jwt.getId(),
                jwt.getIssuedAt() == null ? 0L : jwt.getIssuedAt().getTime() / 1000L,
                jwt.getExpiresAt() == null ? 0L : jwt.getExpiresAt().getTime() / 1000L);
    }
}
