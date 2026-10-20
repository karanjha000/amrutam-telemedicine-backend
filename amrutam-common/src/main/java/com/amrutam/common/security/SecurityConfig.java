package com.amrutam.common.security;

import com.amrutam.common.exception.ApiErrorCode;
import com.amrutam.common.exception.ErrorBody;
import com.amrutam.common.exception.RestResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
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

    private static final RequestMatcher AUTH_JSON_MATCHER =
            request -> request.getServletPath().startsWith("/api/")
                    || request.getServletPath().equals("/graphql");

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthHeaderFilter authHeaderFilter,
            ObjectMapper objectMapper) throws IOException {
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
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, 401,
                                        new ErrorBody(ApiErrorCode.UNAUTHORIZED, "Authentication required")))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, 403,
                                        new ErrorBody(ApiErrorCode.FORBIDDEN, "Access denied"))));
        return http.build();
    }

    /**
     * Delegates to the single {@link org.springframework.security.authentication.AuthenticationProvider}
     * contributed by the api module; kept here so that both
     * {@code @EnableMethodSecurity} and the HTTP chain resolve a consistent manager.
     */
    @Bean
    @Primary
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(
            org.springframework.security.authentication.AuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    static void writeError(HttpServletResponse response, int status, ErrorBody errorBody) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(new String(
                serialize(errorBody).getBytes(StandardCharsets.UTF_8)));
    }

    private static String serialize(ErrorBody errorBody) {
        // Small inline serializer to avoid pulling an ObjectMapper into a
        // static helper; RestResponse has only four fields.
        String message = errorBody.message() == null ? "" :
                errorBody.message().replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"success\":false,\"data\":null,\"error\":{\"code\":\""
                + errorBody.code().name() + "\",\"message\":\"" + message
                + "\"},\"traceId\":null}";
    }

    /**
     * The authentication provider is supplied by the consuming application
     * (the api module); the bean is left unregistered here on purpose so
     * the common module stays free of api-layer domain services.
     */
    static Authentication currentAuthenticationOrNull() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
