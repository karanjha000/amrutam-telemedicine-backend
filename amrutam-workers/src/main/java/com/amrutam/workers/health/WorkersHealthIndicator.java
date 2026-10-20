package com.amrutam.workers.health;

import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.amrutam.workers.config.WorkersProperties;
import com.amrutam.workers.jobs.JobProcessor;

/**
 * Liveness view over the job-processing loop, exposed at
 * {@code /actuator/health}. Reports DOWN when {@link JobProcessor#lastPollAt()}
 * is older than three poll intervals — i.e. the scheduled poller stopped
 * running (or the store is down long enough for three misses).
 */
@Component
public class WorkersHealthIndicator implements HealthIndicator {

    private final JobProcessor processor;
    private final WorkersProperties props;

    /**
     * @param processor the polling loop (exposes lastPollAt)
     * @param props     configuration (poll interval, for staleness threshold)
     */
    public WorkersHealthIndicator(JobProcessor processor, WorkersProperties props) {
        this.processor = processor;
        this.props = props;
    }

    @Override
    public Health health() {
        Instant last = processor.lastPollAt();
        long pollMs = (long) props.getPollIntervalSeconds() * 1000L;
        Instant deadline = Instant.now().minus(pollMs * 3);
        boolean up = last.isAfter(deadline);
        Health.Builder h = up ? Health.up() : Health.down();
        return h.withDetail("pollIntervalMs", pollMs)
                .withDetail("lastPollAt", last.toString())
                .withDetail("stale", !up)
                .build();
    }
}
