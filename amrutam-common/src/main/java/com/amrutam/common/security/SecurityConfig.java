package com.amrutam.common.security;

import com.amrutam.common.exception.ApiErrorCode;
import com.amrutam.common.exception.ErrorBody;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

/**
 * Shared security wiring: stateless JWT filter, HTTP authorization rules and
 * JSON 401/403 responses. Method-level security ({@code @PreAuthorize}) is
 * enabled here and consumed by the api module.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * The shared HTTP security chain.
     *
     * @param http the chain builder
     * @param authHeaderFilter the JWT filter
     * @return the built chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, AuthHeaderFilter authHeaderFilter) throws java.lang.Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(authHeaderFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/actuator/health",
                                "/graphql",
                                "/graphql/schema",
                                "/api/v1/auth/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            writeError(response, 401,
                                    new ErrorBody(ApiErrorCode.UNAUTHORIZED, "Authentication required"));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            writeError(response, 403,
                                    new ErrorBody(ApiErrorCode.FORBIDDEN, "Access denied"));
                        }));
        return http.build();
    }

    /**
     * Delegates to the single AuthenticationProvider contributed by the
     * api module; both {@code @EnableMethodSecurity} and the HTTP chain
     * resolve this consistent manager.
     *
     * @param authenticationProvider the API-layer provider bean
     * @return the manager
     */
    @Bean
    @Primary
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(
            org.springframework.security.authentication.AuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    /**
     * The shared password encoder.
     *
     * @return a BCrypt encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    static void writeError(HttpServletResponse response, int status, ErrorBody errorBody) {
        try {
            response.setStatus(status);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String message =
                    errorBody.message() == null
                            ? ""
                            : errorBody.message().replace("\\", "\\\\").replace("\"", "\\\"");
            String json =
                    "{\"success\":false,\"data\":null,\"error\":{\"code\":\""
                            + errorBody.code().name()
                            + "\",\"message\":\""
                            + message
                            + "\"},\"traceId\":null}";
            response.getWriter().write(json);
        } catch (java.io.IOException e) {
            // Nothing useful to do if the response can't be written — the client
            // is already gone; log and drop.
            org.slf4j.LoggerFactory.getLogger(SecurityConfig.class)
                    .warn("Could not write error response: {}", e.getMessage());
        }
    }

    /**
     * Convenient accessor used by downstream code/tests.
     *
     * @return the current authentication or null
     */
    static Authentication currentAuthenticationOrNull() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
