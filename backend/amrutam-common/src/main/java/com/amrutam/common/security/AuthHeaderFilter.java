package com.amrutam.common.security;

import com.amrutam.common.exception.AmrutamException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads the {@code Authorization: Bearer <token>} header, validates it with
 * the {@link JwTokenService} and, on success, publishes the parsed
 * {@link Claims} as a request attribute and installs a
 * {@link UsernamePasswordAuthenticationToken} into the
 * {@link SecurityContextHolder}.
 *
 * <p>When no (or an invalid) token is present the filter does NOT short-circuit:
 * the chain proceeds unauthenticated so that downstream rules (permitAll
 * paths, auth endpoints) still work. Actuator/Swagger docs are skipped
 * entirely.
 */
public class AuthHeaderFilter extends OncePerRequestFilter {

    /**
     * Request attribute key under which the parsed {@link Claims} are exposed.
     */
    public static final String AUTH_CLAIMS_ATTR = AuthHeaderFilter.class.getName() + ".CLAIMS";

    /**
     * Convenience alias for the same attribute under a short key, so that
     * consuming code does not have to repeat the FQCN.
     */
    public static final String AUTH_CLAIMS_ALIASED_ATTR = "amrutam.authClaims";

    private static final Logger log = LoggerFactory.getLogger(AuthHeaderFilter.class);

    private static final List<String> SKIP_PATH_PREFIXES =
            List.of("/v3/api-docs", "/swagger-ui", "/actuator");

    private final JwTokenService jwTokenService;

    /**
     * Creates the filter.
     *
     * @param jwTokenService the token service used for validation
     */
    public AuthHeaderFilter(JwTokenService jwTokenService) {
        this.jwTokenService = jwTokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractBearerToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        JwTokenService.Claims claims = validateOrNull(token);
        if (claims != null) {
            request.setAttribute(AUTH_CLAIMS_ATTR, claims);
            request.setAttribute(AUTH_CLAIMS_ALIASED_ATTR, claims);
            SecurityContextHolder.getContext().setAuthentication(authenticationFor(claims));
        }
        // Missing/invalid: do NOT wipe the context (nothing was set in the
        // first place) and do NOT short-circuit; let the authorization rules
        // downstream decide whether the path requires auth.
        filterChain.doFilter(request, response);
    }

    private static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = header.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private JwTokenService.Claims validateOrNull(String token) {
        try {
            return jwTokenService.parseAndValidate(token);
        } catch (AmrutamException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return null;
        }
    }

    private static UsernamePasswordAuthenticationToken authenticationFor(JwTokenService.Claims claims) {
        List<SimpleGrantedAuthority> authorities =
                claims.roles().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        return UsernamePasswordAuthenticationToken.authenticated(
                String.valueOf(claims.userId()), null, authorities);
    }
}
