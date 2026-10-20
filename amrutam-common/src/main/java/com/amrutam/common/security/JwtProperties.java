package com.amrutam.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the platform JWT issuer.
 *
 * <p>{@code secret} is an HMAC-SHA256 secret; the default value is a
 * development-only placeholder and MUST be overridden with a strong,
 * randomly generated value in any non-dev environment.
 */
@ConfigurationProperties(prefix = "amrutam.jwt")
public class JwtProperties {

    /** Placeholder dev-only HMAC secret. Override per environment. */
    public static final String DEV_SECRET =
            "amrutam-dev-hmac-secret-change-me-0123456789abcdef";

    private String secret = DEV_SECRET;
    private String issuer = "amrutam";
    private long ttlSeconds = 900L;
    private int clockSkewSeconds = 30;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public int getClockSkewSeconds() {
        return clockSkewSeconds;
    }

    public void setClockSkewSeconds(int clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
    }
}
