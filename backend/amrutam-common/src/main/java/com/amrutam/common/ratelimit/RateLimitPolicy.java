package com.amrutam.common.ratelimit;

/**
 * A rate limit: a sustained rate plus a short burst allowance.
 *
 * @param qps       requests per second
 * @param burstSize how many tokens may be held (the bucket capacity)
 */
public record RateLimitPolicy(int qps, int burstSize) {

    /**
     * Requires a positive sustained rate and a non-negative burst.
     */
    public RateLimitPolicy {
        if (qps <= 0) {
            throw new IllegalArgumentException("qps must be positive");
        }
        if (burstSize < 0) {
            throw new IllegalArgumentException("burstSize must be >= 0");
        }
    }
}
