package com.amrutam.common.ratelimit;

import com.amrutam.common.exception.ApiErrorCode;
import com.amrutam.common.exception.ErrorBody;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-endpoint rate limiting via token buckets. One bucket per request path
 * is lazily created; the policy is resolved from the {@link RateLimitConfig}
 * overrides, falling back to the defaults.
 *
 * <p>Exhausted requests receive a 429 {@code RestResponse} body and a
 * {@code Retry-After: 1} header.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String RETRY_AFTER = "Retry-After";

    private final RateLimitConfig config;
    private final Map<String, TokenBucketRateLimiter> buckets = new ConcurrentHashMap<>();

    /**
     * Creates the filter.
     *
     * @param config the rate limit configuration
     */
    public RateLimitFilter(RateLimitConfig config) {
        this.config = config;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        TokenBucketRateLimiter bucket = bucketFor(request.getServletPath());
        if (!bucket.tryAcquire()) {
            write429(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private TokenBucketRateLimiter bucketFor(String path) {
        return buckets.computeIfAbsent(
                path,
                p -> {
                    RateLimitPolicy policy =
                            config.getOverrides().getOrDefault(
                                    p,
                                    new RateLimitPolicy(
                                            config.getDefaultQps(), config.getDefaultBurst()));
                    return new TokenBucketRateLimiter(policy, policy.burstSize());
                });
    }

    private void write429(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setHeader(RETRY_AFTER, "1");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ErrorBody body =
                new ErrorBody(ApiErrorCode.RATE_LIMITED, "Too many requests, retry later");
        response.getWriter().write(render(body));
    }

    private static String render(ErrorBody errorBody) {
        String message =
                errorBody.message() == null
                        ? ""
                        : errorBody.message().replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"success\":false,\"data\":null,\"error\":{\"code\":\""
                + errorBody.code().name()
                + "\",\"message\":\""
                + message
                + "\"},\"traceId\":null}";
    }

    /**
     * Exposes the bucket map for tests/metrics.
     *
     * @return the path to limiter map
     */
    public Map<String, TokenBucketRateLimiter> buckets() {
        return buckets;
    }
}
