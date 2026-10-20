package com.amrutam.api.config;

import com.amrutam.api.workerbridge.AsyncBridge;
import com.amrutam.api.workerbridge.JdbcAsyncBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Provides the default {@link AsyncBridge} used by the api module to enqueue
 * asynchronous work.
 *
 * <p>The {@link JdbcAsyncBridge} {@code @Component} is the implementation;
 * this configuration publishes it as the {@code @Primary} {@link AsyncBridge}
 * bean so that, when the workers module registers its own
 * {@code @ConditionalOnProperty(name="amrutam.workers.enabled",
 * havingValue="true")} {@link AsyncBridge} bean, that one wins and this
 * default falls back gracefully.</p>
 */
@Configuration
public class AsyncBridgeConfig {

    /**
     * Default {@link AsyncBridge} backed by direct JPA writes to
     * {@code async_jobs}.
     *
     * @param bridge the JPA-backed implementation
     * @return the primary bridge
     */
    @Bean
    @Primary
    public AsyncBridge asyncBridge(JdbcAsyncBridge bridge) {
        return bridge;
    }
}
