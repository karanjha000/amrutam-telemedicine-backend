package com.amrutam.api.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.AbstractCacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.cache.Cache.ValueWrapper;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Minimal, dependency-free {@link CacheManager} used when Redis-backed
 * caching is disabled ({@code amrutam.cache.redis-enabled=false}).
 *
 * <p>Entries live in a {@link ConcurrentHashMap} with a TTL enforced lazily
 * via {@link System#currentTimeMillis()} on read — no background threads,
 * no external dependencies. Each cache keeps its own TTL; lookups expire
 * per cache independently. Suitable for dev / tests where cross-instance
 * consistency is not required.</p>
 */
public class InMemoryCacheManager extends AbstractCacheManager {

    private final Map<String, InMemoryCache> caches = new ConcurrentHashMap<>();

    /** Per-cache TTLs, registered via {@link #registerCache(String, Duration)}. */
    private final Map<String, Duration> ttlByCache = new LinkedHashMap<>();

    /**
     * Registers a default TTL for the named cache.
     *
     * @param name cache name
     * @param ttl  time-to-live
     */
    public void registerCache(String name, Duration ttl) {
        ttlByCache.put(name, ttl);
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, this::newCache);
    }

    private InMemoryCache newCache(String name) {
        Duration ttl = ttlByCache.getOrDefault(name, Duration.ofMinutes(10));
        return new InMemoryCache(name, ttl);
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        // Caches are created on demand via {@link #getCache(String)}; nothing to preload.
        return List.of();
    }

    private final class InMemoryCache implements Cache {

        private final String name;
        private final Duration ttl;
        private final ConcurrentMap<String, ExpiringCacheValue> entries = new ConcurrentHashMap<>();

        private InMemoryCache(String name, Duration ttl) {
            this.name = name;
            this.ttl = ttl;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return entries;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Class<T> type) {
            ExpiringCacheValue v = peek(key);
            return v == null ? null : type.cast(v.value());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            ExpiringCacheValue v = peek(key);
            if (v != null) {
                return (T) v.value();
            }
            T loaded;
            try {
                loaded = valueLoader.call();
            } catch (Exception e) {
                throw new Cache.ValueRetrievalException(key, valueLoader, e);
            }
            put(key, loaded);
            return loaded;
        }

        @Override
        public ValueWrapper get(Object key) {
            ExpiringCacheValue v = peek(key);
            return v == null ? null : new SimpleValueWrapper(v.value());
        }

        @Override
        public void put(Object key, Object value) {
            entries.put(String.valueOf(key),
                    new ExpiringCacheValue(value, System.currentTimeMillis() + ttl.toMillis()));
        }

        @Override
        public void evict(Object key) {
            entries.remove(String.valueOf(key));
        }

        @Override
        public void clear() {
            entries.clear();
        }

        private ExpiringCacheValue peek(Object key) {
            ExpiringCacheValue v = entries.get(String.valueOf(key));
            if (v == null) {
                return null;
            }
            if (v.expiresAt() <= System.currentTimeMillis()) {
                entries.remove(String.valueOf(key));
                return null;
            }
            return v;
        }

        private record ExpiringCacheValue(Object value, long expiresAt) {
        }
    }
}
