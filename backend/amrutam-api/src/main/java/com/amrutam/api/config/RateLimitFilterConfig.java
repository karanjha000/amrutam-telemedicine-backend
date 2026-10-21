package com.amrutam.api.config;

import com.amrutam.common.ratelimit.RateLimitConfig;
import com.amrutam.common.ratelimit.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the common kernel's {@link RateLimitFilter} as a servlet filter.
 *
 * <p>The {@code RateLimitConfig} properties bean is contributed by the common
 * {@code CommonAutoConfiguration} ({@code @EnableConfigurationProperties});
 * this configuration publishes the filter itself. Spring Boot auto-registers
 * every {@code Filter}-typed bean in the servlet chain (via its
 * {@code ServletContextInitializer}), so a plain {@code @Bean} is enough —
 * no explicit {@code FilterRegistrationBean} is required.</p>
 */
@Configuration
public class RateLimitFilterConfig {

    /**
     * The per-endpoint token-bucket rate limiter.
     *
     * @param props the bound {@code amrutam.ratelimit.*} properties
     * @return the filter
     */
    @Bean
    public RateLimitFilter rateLimitFilter(RateLimitConfig props) {
        return new RateLimitFilter(props);
    }
}
