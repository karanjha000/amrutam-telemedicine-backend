package com.amrutam.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.amrutam.common.idempotency.DefaultIdempotencyService;
import com.amrutam.common.idempotency.IdempotencyRepository;
import com.amrutam.common.ratelimit.RateLimitConfig;
import com.amrutam.common.security.AuthHeaderFilter;
import com.amrutam.common.security.JwTokenService;
import com.amrutam.common.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-wires the reusable cross-cutting beans so that consuming modules
 * only need to depend on the SPIs (e.g. {@link IdempotencyRepository})
 * instead of the concrete implementations.
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, RateLimitConfig.class})
public class CommonAutoConfiguration {

    /**
     * The JWT authentication filter.
     *
     * @param jwt the token service
     * @return the filter
     */
    @Bean
    public AuthHeaderFilter authHeaderFilter(JwTokenService jwt) {
        return new AuthHeaderFilter(jwt);
    }

    /**
     * The default idempotency service, used when the application has not
     * supplied its own.
     *
     * @param objectMapper the platform mapper
     * @param repository   the storage SPI
     * @return the service
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultIdempotencyService defaultIdempotencyService(
            ObjectMapper objectMapper, IdempotencyRepository repository) {
        return new DefaultIdempotencyService(repository, objectMapper);
    }
}
