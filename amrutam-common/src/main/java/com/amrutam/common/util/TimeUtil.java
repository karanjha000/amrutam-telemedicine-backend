package com.amrutam.common.util;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Time helpers that centralize the UTC convention used across the platform.
 */
public final class TimeUtil {

    /** The platform timezone offset. */
    public static final ZoneOffset UTC = ZoneOffset.UTC;

    private TimeUtil() {
    }

    /**
     * The current instant.
     *
     * @return now
     */
    public static Instant now() {
        return Instant.now();
    }
}
