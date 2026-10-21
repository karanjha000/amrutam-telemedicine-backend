package com.amrutam.common.exception;

/**
 * The {@code error} member of a {@link RestResponse}: a machine-readable
 * code plus a human-readable message.
 *
 * @param code    the error code
 * @param message the human-readable message
 */
public record ErrorBody(ApiErrorCode code, String message) {
}
