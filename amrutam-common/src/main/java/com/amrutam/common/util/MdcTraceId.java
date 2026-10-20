package com.amrutam.common.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * MDC helpers for the per-request trace id.
 */
public final class MdcTraceId {

    /** MDC key carrying the current trace id. */
    public static final String KEY = "traceId";

    private MdcTraceId() {
    }

    /**
     * Generates a new 16-char lowercase trace id (UUID without dashes,
     * truncated).
     *
     * @return a fresh trace id
     */
    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Assigns a fresh trace id to the current thread's MDC, replacing any
     * existing value.
     */
    public static void assign() {
        MDC.put(KEY, newTraceId());
    }

    /**
     * The current trace id, or null when not set.
     *
     * @return the trace id
     */
    public static String current() {
        return MDC.get(KEY);
    }

    /**
     * Clears the trace id from MDC.
     */
    public static void clear() {
        MDC.remove(KEY);
    }
}
