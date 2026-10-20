package com.amrutam.common.ratelimit;

/**
 * A simple admission-control interface.
 */
public interface RateLimiter {

    /**
     * Attempts to acquire a permit without blocking.
     *
     * @return true if a permit was granted
     */
    boolean tryAcquire();

    /**
     * Blocks until a permit is granted (best-effort: implementations may
     * simply call {@link #tryAcquire()} in a tight loop when a sensible
     * wait time is unknown).
     */
    void acquire();
}
