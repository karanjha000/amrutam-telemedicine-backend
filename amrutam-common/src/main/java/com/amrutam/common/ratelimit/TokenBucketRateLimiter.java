package com.amrutam.common.ratelimit;

import java.time.Clock;

/**
 * A classic token-bucket rate limiter, pure Java (no third-party
 * dependency). Thread-safe via synchronized state.
 *
 * <p>The bucket starts with {@code initialTokens} (capped at the policy
 * burst size) and refills at {@code qps} tokens/second up to the burst
 * capacity.
 */
public class TokenBucketRateLimiter implements RateLimiter {

    private final int capacity;
    private final double qps;
    private final Clock clock;

    private double tokens;
    private long lastRefillMillis;

    /**
     * Creates a limiter with the system UTC clock.
     *
     * @param policy        the policy (capacity = burstSize, refill = qps)
     * @param initialTokens starting token count, clamped to [0, capacity]
     */
    public TokenBucketRateLimiter(RateLimitPolicy policy, int initialTokens) {
        this(policy, initialTokens, Clock.systemUTC());
    }

    /**
     * Creates a limiter with an injectable clock (used by tests).
     *
     * @param policy        the policy
     * @param initialTokens starting token count, clamped to [0, capacity]
     * @param clock         the clock used for refill timing
     */
    public TokenBucketRateLimiter(RateLimitPolicy policy, int initialTokens, Clock clock) {
        this.capacity = Math.max(policy.burstSize(), 1);
        this.qps = policy.qps();
        this.clock = clock;
        this.tokens = Math.max(0.0, Math.min(initialTokens, (double) this.capacity));
        this.lastRefillMillis = clock.millis();
    }

    @Override
    public synchronized boolean tryAcquire() {
        refill(clock.millis());
        if (tokens < 1.0) {
            return false;
        }
        tokens -= 1.0;
        return true;
    }

    @Override
    public synchronized void acquire() {
        while (!tryAcquire()) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void refill(long nowMillis) {
        long elapsedMillis = nowMillis - lastRefillMillis;
        if (elapsedMillis <= 0) {
            return;
        }
        double newTokens = (elapsedMillis / 1000.0) * qps;
        tokens = Math.min(capacity, tokens + newTokens);
        lastRefillMillis = nowMillis;
    }
}
