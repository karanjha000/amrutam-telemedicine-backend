package com.amrutam.api.exception;

import com.amrutam.common.exception.AmrutamException;
import com.amrutam.common.exception.BadRequestException;
import com.amrutam.common.exception.ConflictException;
import com.amrutam.common.exception.ErrorBody;
import com.amrutam.common.exception.ApiErrorCode;
import com.amrutam.common.exception.ForbiddenException;
import com.amrutam.common.exception.NotFoundException;
import com.amrutam.common.exception.RestResponse;
import com.amrutam.common.security.JwtException;
import com.amrutam.common.util.MdcTraceId;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps the shared {@code AmrutamException} hierarchy (plus Spring MVC /
 * Security exceptions) onto {@link RestResponse} envelopes and HTTP status
 * codes.
 *
 * <p>Every handler guarantees the MDC {@code traceId} is present
 * ({@link MdcTraceId#assign()} when missing) so the traceId embedded in the
 * envelope matches the logging context. Error bodies keep machine-readable
 * {@link ApiErrorCode}s and only leak messages for expected (domain)
 * exceptions; the catch-all hides internals and logs the stack trace.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ObjectMapper json;

    /**
     * @param json json mapper (serializes field-error details into 400 bodies)
     */
    public GlobalExceptionHandler(ObjectMapper json) {
        this.json = json;
    }

    /** Domain 404. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<RestResponse<Void>> notFound(NotFoundException e) {
        return error(ApiErrorCode.NOT_FOUND, e.getMessage());
    }

    /** Domain 409 (includes in-progress idempotency conflicts). */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<RestResponse<Void>> conflict(ConflictException e) {
        ApiErrorCode code = inProgress(e) ? ApiErrorCode.IDEMPOTENCY_REPLAY_CONFLICT : ApiErrorCode.CONFLICT;
        return error(code, e.getMessage());
    }

    /** Domain 403. */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<RestResponse<Void>> forbidden(ForbiddenException e) {
        return error(ApiErrorCode.FORBIDDEN, e.getMessage());
    }

    /** Domain 400. */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<RestResponse<Void>> badRequest(BadRequestException e) {
        return error(ApiErrorCode.BAD_REQUEST, e.getMessage());
    }

    /** 401 for JWT failures (expired / invalid signatures). */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<RestResponse<Void>> jwtFailed(JwtException e) {
        return error(ApiErrorCode.UNAUTHORIZED, e.getMessage());
    }

    /**
     * Any other {@link AmrutamException} subtype without a dedicated
     * handler (e.g. future domain exceptions): 400, message surfaced.
     */
    @ExceptionHandler(AmrutamException.class)
    public ResponseEntity<RestResponse<Void>> amrutam(AmrutamException e) {
        return error(ApiErrorCode.BAD_REQUEST, e.getMessage());
    }

    /** Spring Security: missing/insufficient credentials at the HTTP level. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RestResponse<Void>> authentication(AuthenticationException e) {
        return error(ApiErrorCode.UNAUTHORIZED, "unauthorized");
    }

    /** Spring Security: {@code @PreAuthorize} denial or service-level 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RestResponse<Void>> accessDenied(AccessDeniedException e) {
        return error(ApiErrorCode.FORBIDDEN, e.getMessage());
    }

    /** Bean Validation on the request body: 400 with per-field detail. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Void>> bodyValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return error(ApiErrorCode.BAD_REQUEST, "validation failed: " + toJson(fieldErrors));
    }

    /** Bean Validation on path/query params: 400. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RestResponse<Void>> paramValidation(ConstraintViolationException e) {
        return error(ApiErrorCode.BAD_REQUEST, e.getMessage());
    }

    /** Malformed / missing request body: 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RestResponse<Void>> unreadable(HttpMessageNotReadableException e) {
        return error(ApiErrorCode.BAD_REQUEST, "Malformed request body");
    }

    /** Unique-constraint race (duplicate username, duplicate slot window): 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RestResponse<Void>> integrity(DataIntegrityViolationException e) {
        log.debug("DataIntegrityViolationException: {}", e.getMostSpecificCause().getMessage());
        return error(ApiErrorCode.CONFLICT, "duplicate or inconsistent data");
    }

    /** Unsupported HTTP method on a known resource: 405. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RestResponse<Void>> notSupported(HttpRequestMethodNotSupportedException e) {
        return error(ApiErrorCode.BAD_REQUEST, "method not supported: " + e.getMethod());
    }

    /** Catch-all: 500, stack trace logged, no internals leaked to the caller. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Void>> unexpected(Exception e) {
        if (MdcTraceId.current() == null) {
            MdcTraceId.assign();
        }
        log.error("Unhandled exception traceId={}", MdcTraceId.current(), e);
        return error(ApiErrorCode.INTERNAL, "internal server error");
    }

    private static boolean inProgress(ConflictException e) {
        String m = e.getMessage();
        return m != null && m.contains("in progress");
    }

    private ResponseEntity<RestResponse<Void>> error(ApiErrorCode code, String message) {
        if (MdcTraceId.current() == null) {
            MdcTraceId.assign();
        }
        return ResponseEntity.status(code.toHttpStatus())
                .body(RestResponse.error(new ErrorBody(code, message)));
    }

    private String toJson(Map<String, String> fieldErrors) {
        try {
            return json.writeValueAsString(fieldErrors);
        } catch (Exception ex) {
            return String.valueOf(fieldErrors);
        }
    }
}
