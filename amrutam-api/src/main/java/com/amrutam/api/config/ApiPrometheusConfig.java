package com.amrutam.api.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Prometheus / Micrometer configuration.
 *
 * <p>Tags every emitted metric with {@code service=amrutam-api} and
 * {@code apiVersion=v1} so dashboards can split multi-service deployments
 * without scraping different endpoints. The filter only adds a tag when it
 * is not already present, so code that tags metrics explicitly is respected.</p>
 */
@Configuration
public class ApiPrometheusConfig {

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
                List<Tag> tags = new ArrayList<>();
                id.getTags().forEach(tags::add);
                Tags merged = Tags.of(tags);
                if (!hasTag(tags, "service")) {
                    merged = merged.and("service", "amrutam-api");
                }
                if (!hasTag(tags, "apiVersion")) {
                    merged = merged.and("apiVersion", "v1");
                }
                return id.replaceTags(merged);
            }

            @Override
            public MeterFilterReply accept(Meter.Id id) {
                return MeterFilterReply.ACCEPT;
            }

            private boolean hasTag(List<Tag> tags, String key) {
                for (Tag t : tags) {
                    if (key.equals(t.getKey())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
