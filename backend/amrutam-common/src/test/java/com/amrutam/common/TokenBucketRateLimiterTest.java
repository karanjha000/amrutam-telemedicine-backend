package com.amrutam.common;

import com.amrutam.common.ratelimit.RateLimitPolicy;
import com.amrutam.common.ratelimit.TokenBucketRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBucketRateLimiterTest {

    private static final class MovableClock extends Clock {
        private Instant instant;

        MovableClock(Instant start) {
            this.instant = start;
        }

        Instant advanceTo(Instant newInstant) {
            this.instant = newInstant;
            return newInstant;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    @Test
    void allowsUpToBurstThenBlocks() {
        RateLimitPolicy policy = new RateLimitPolicy(1, 3);
        MovableClock clock = new MovableClock(Instant.parse("2026-01-01T00:00:00Z"));
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(policy, 3, clock);

        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();
    }

    @Test
    void advancingClockRefillsTokens() {
        RateLimitPolicy policy = new RateLimitPolicy(2, 4);
        MovableClock clock = new MovableClock(Instant.parse("2026-01-01T00:00:00Z"));
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(policy, 4, clock);

        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();

        clock.advanceTo(clock.instant().plusSeconds(1));
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isFalse();

        clock.advanceTo(clock.instant().plusSeconds(1));
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
    }
}
