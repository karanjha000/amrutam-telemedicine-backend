package com.amrutam.api.config;

import com.amrutam.api.cache.InMemoryCacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

/**
 * Caching configuration.
 *
 * <p>Two strategies, selected by {@code amrutam.cache.redis-enabled}
 * (default {@code true} when the property is absent):</p>
 * <ul>
 *   <li><b>Redis-backed</b> ({@link RedisCacheManager}) with per-cache TTLs:
 *       {@code users=5m, doctors=15m, availability=60s, consultations=30s,
 *       analytics=5m}.</li>
 *   <li><b>In-memory fallback</b> ({@link InMemoryCacheManager}) – a plain
 *       {@link java.util.concurrent.ConcurrentHashMap}-based manager with the
 *       same TTLs, used when Redis is unavailable (dev / tests).</li>
 * </ul>
 */
@Configuration
public class CacheConfig {

    /** Per-cache TTLs applied to both Redis and in-memory managers. */
    private static final Map<String, Duration> CACHE_TTLS = Map.of(
            "users", Duration.ofMinutes(5),
            "doctors", Duration.ofMinutes(15),
            "availability", Duration.ofSeconds(60),
            "consultations", Duration.ofSeconds(30),
            "analytics", Duration.ofMinutes(5));

    /**
     * Redis-backed cache manager with per-cache TTLs.
     *
     * @param factory redis connection factory (from Spring Boot autoconfig)
     * @return the cache manager
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "amrutam.cache", name = "redis-enabled",
            havingValue = "true", matchIfMissing = true)
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheManager.RedisCacheManagerBuilder builder =
                RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(factory)
                        .cacheDefaults(defaultConfiguration(Duration.ofMinutes(10)));
        for (Map.Entry<String, Duration> e : CACHE_TTLS.entrySet()) {
            builder.withCacheConfiguration(e.getKey(), defaultConfiguration(e.getValue()));
        }
        return builder.build();
    }

    /**
     * In-memory fallback cache manager (same TTLs) when Redis is disabled.
     *
     * @return the cache manager
     */
    @Bean
    @ConditionalOnProperty(prefix = "amrutam.cache", name = "redis-enabled",
            havingValue = "false")
    public CacheManager inMemoryCacheManager() {
        InMemoryCacheManager manager = new InMemoryCacheManager();
        for (Map.Entry<String, Duration> e : CACHE_TTLS.entrySet()) {
            manager.registerCache(e.getKey(), e.getValue());
        }
        return manager;
    }

    private static RedisCacheConfiguration defaultConfiguration(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
