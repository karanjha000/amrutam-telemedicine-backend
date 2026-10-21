package com.amrutam.common.exception;

import org.slf4j.MDC;

/**
 * Standard response envelope for all Amrutam HTTP endpoints.
 *
 * <p>Successful responses carry {@code success=true}, a payload and a null
 * {@code error}. Failure responses carry {@code success=false}, a null data
 * payload and a populated {@link ErrorBody}.
 *
 * @param <T>     the payload type
 * @param success whether the request succeeded
 * @param data    the payload (nullable on failure)
 * @param error   the error body (nullable on success)
 * @param traceId the MDC trace id for request correlation
 */
public record RestResponse<T>(boolean success, T data, ErrorBody error, String traceId) {

    private static final String TRACE_ID_MDC_KEY = "traceId";

    /**
     * Builds a success envelope whose trace id is read from MDC.
     *
     * @param <T>  the payload type
     * @param data the payload
     * @return a success envelope
     */
    public static <T> RestResponse<T> ok(T data) {
        return new RestResponse<>(true, data, null, MDC.get(TRACE_ID_MDC_KEY));
    }

    /**
     * Alias for {@link #ok(Object)}.
     *
     * @param <T>  the payload type
     * @param data the payload
     * @return a success envelope
     */
    public static <T> RestResponse<T> of(T data) {
        return ok(data);
    }

    /**
     * Builds a failure envelope.
     *
     * @param <T> the (null) payload type, inferred from the caller's target
     * @param e   the error body
     * @return a failure envelope with a null payload
     */
    public static <T> RestResponse<T> error(ErrorBody e) {
        return new RestResponse<>(false, null, e, MDC.get(TRACE_ID_MDC_KEY));
    }
}
