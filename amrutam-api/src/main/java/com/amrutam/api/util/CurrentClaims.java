package com.amrutam.api.util;

import com.amrutam.common.security.AuthHeaderFilter;
import com.amrutam.common.security.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Reads the validated {@link Claims} published by the common kernel's
 * {@link AuthHeaderFilter} (request attribute {@link
 * AuthHeaderFilter#AUTH_CLAIMS_ATTR}) so controllers can act on the
 * validated user id / roles instead of re-deriving them from the
 * {@code Authentication} principal name (which is the numeric user id, not a
 * human-readable username).
 */
@Component
public class CurrentClaims {

    /**
     * The parsed claims for the current request.
     *
     * @param request the request
     * @return the claims, or {@code null} when no valid bearer token was
     *         presented
     */
    public Claims claims(HttpServletRequest request) {
        Object value = request.getAttribute(AuthHeaderFilter.AUTH_CLAIMS_ATTR);
        return value instanceof Claims c ? c : null;
    }

    /**
     * The acting user id (validated claim, never a spoofable string).
     *
     * @param request the request
     * @return the user id, or {@code null} when unauthenticated
     */
    public java.util.UUID userId(HttpServletRequest request) {
        Claims c = claims(request);
        return c == null ? null : c.userId();
    }
}
