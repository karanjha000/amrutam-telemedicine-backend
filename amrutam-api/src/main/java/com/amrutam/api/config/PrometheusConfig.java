package com.amrutam.api.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prometheus / Micrometer configuration.
 *
 * <p>Tags every emitted metric with {@code service=amrutam-api} and
 * {@code apiVersion=v1} so dashboards can split multi-service deployments
 * without scraping different endpoints. The filter only adds a tag when it
 * is not already present, so code that tags metrics explicitly is respected.</p>
 */
@Configuration
public class PrometheusConfig {

    /**
     * Meter filter merging the default service tags into every meter.
     *
     * @return the filter
     */
    @Bean
    public MeterFilter amrutamServiceTagFilter() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                Tags merged = id.getTags();
                if (!hasTag(id, "service")) {
                    merged = merged.and("service", "amrutam-api");
                }
                if (!hasTag(id, "apiVersion")) {
                    merged = merged.and("apiVersion", "v1");
                }
                return id.withTags(merged);
            }

            @Override
            public boolean accept(Meter.Id id) {
                return true;
            }

            private boolean hasTag(Meter.Id id, String key) {
                for (Tag t : id.getTags()) {
                    if (key.equals(t.getKey())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
