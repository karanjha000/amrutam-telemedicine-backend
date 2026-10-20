package com.amrutam.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration for per-endpoint rate limiting.
 *
 * <pre>
 * amrutam:
 *   ratelimit:
 *     default-qps: 20
 *     default-burst: 40
 *     overrides:
 *       "/api/v1/appointments":
 *         qps: 5
 *         burst-size: 10
 * </pre>
 */
@ConfigurationProperties(prefix = "amrutam.ratelimit")
public class RateLimitConfig {

    private int defaultQps = 20;
    private int defaultBurst = 40;
    private Map<String, RateLimitPolicy> overrides = new LinkedHashMap<>();

    public int getDefaultQps() {
        return defaultQps;
    }

    public void setDefaultQps(int defaultQps) {
        this.defaultQps = defaultQps;
    }

    public int getDefaultBurst() {
        return defaultBurst;
    }

    public void setDefaultBurst(int defaultBurst) {
        this.defaultBurst = defaultBurst;
    }

    public Map<String, RateLimitPolicy> getOverrides() {
        return overrides;
    }

    public void setOverrides(Map<String, RateLimitPolicy> overrides) {
        this.overrides = overrides == null ? new LinkedHashMap<>() : overrides;
    }
}
