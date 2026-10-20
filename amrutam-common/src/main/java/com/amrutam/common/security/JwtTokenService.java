package com.amrutam.common.security;

import com.amrutam.common.exception.AmrutamException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
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
    private final String issuer;
    private final long defaultTtlSeconds;
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
        this.algorithm = Algorithm.HMAC256(key(props.getSecret()));
        this.issuer = props.getIssuer();
        this.defaultTtlSeconds = props.getTtlSeconds();
        this.clockSkewSeconds = props.getClockSkewSeconds();
        this.clock = clock;
    }

    private static SecretKey key(String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        // Derive a 32-byte key so the length is explicit rather than "whatever
        // the caller configured"; short secrets are padded, long ones truncated.
        byte[] normalized = new byte[32];
        System.arraycopy(bytes, 0, normalized, 0, Math.min(bytes.length, 32));
        return new SecretKeySpec(normalized, "HmacSHA256");
    }

    @Override
    public String issue(Subject subject, String jti, long ttlSeconds) {
        long now = clock.millis() / 1000L;
        return newBuilder(subject, jti)
                .withIssuedAt(new java.util.Date(now * 1000L))
                .withExpiresAt(new java.util.Date((now + ttlSeconds) * 1000L))
                .sign(algorithm);
    }

    private com.auth0.jwt.JWT.Builder newBuilder(Subject subject, String jti) {
        com.auth0.jwt.JWT.Builder builder = JWT.create()
                .withIssuer(issuer)
                .withSubject(subject.username())
                .withClaim(CLAIM_USER_ID, subject.userId().toString())
                .withClaim(CLAIM_ROLES, subject.roles());
        if (jti != null) {
            builder.withJWTId(jti);
        }
        return builder;
    }

    @Override
    public Claims parseAndValidate(String token) {
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(issuer)
                .withAllowedClockSkewSeconds(clockSkewSeconds)
                .build();
        DecodedJWT jwt;
        try {
            jwt = verifier.verify(token);
        } catch (com.auth0.jwt.exceptions.TokenExpiredException e) {
            throw JwtException.expired("Token expired: " + e.getMessage());
        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            throw JwtException.unauthorized("Invalid token: " + e.getMessage());
        }
        return toClaims(jwt);
    }

    @Override
    public boolean canParse(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt != null && jwt.getPayload() != null;
        } catch (AmrutamException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims toClaims(DecodedJWT jwt) {
        UUID userId = UUID.fromString(jwt.getClaim(CLAIM_USER_ID).asString());
        List<String> roles =
                jwt.getClaim(CLAIM_ROLES).asList(String.class) == null
                        ? List.of()
                        : List.copyOf(jwt.getClaim(CLAIM_ROLES).asList(String.class));
        return new Claims(
                userId,
                roles,
                jwt.getId(),
                jwt.getIssuedAt() == null ? 0L : jwt.getIssuedAt().getTime() / 1000L,
                jwt.getExpiresAt() == null ? 0L : jwt.getExpiresAt().getTime() / 1000L);
    }
}
