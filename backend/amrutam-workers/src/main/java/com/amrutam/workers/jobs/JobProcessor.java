package com.amrutam.workers.jobs;

import com.amrutam.workers.config.WorkersProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The async_jobs polling loop.
 *
 * <p>Every poll cycle (fixed delay, default {@code 5000} ms) it fetches due PENDING jobs via the
 * {@link JobStore} (raw JdbcTemplate against the shared table — workers does
 * <em>not</em> own {@code AsyncJobRepository}, that is amrutam-api's),
 * claims each one with a time-boxed lease, hands it to the matching
 * {@link JobHandler} through {@link NotificationDispatcher}, then marks it
 * DONE or re-schedules it with exponential backoff
 * ({@code nextRun = now + min(base * 2^attempt, cap)}). After
 * {@code maxRetries} failed attempts the job is dead-lettered: status set to
 * DEAD, and the operator-visible reason is logged here (the shared schema
 * has no dedicated error column, see {@link JobStore#deadLetter}).</p>
 *
 * <p>The CAS on claim means two workers polling the same table at the same
 * time will not run the same job twice.</p>
 */
@Component
@ConditionalOnProperty(name = "amrutam.workers.enabled", havingValue = "true", matchIfMissing = true)
public class JobProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobProcessor.class);

    private final JobStore jobStore;
    private final NotificationDispatcher dispatcher;
    private final WorkersProperties props;
    private final ObjectMapper json;

    private final String owner;
    private final AtomicReference<Instant> lastPollAt = new AtomicReference<>(Instant.EPOCH);

    /**
     * @param jobStore  raw JDBC job store
     * @param dispatcher handler registry
     * @param props     workers configuration
     * @param json      Jackson mapper for payload (de)serialization
     */
    public JobProcessor(JobStore jobStore, NotificationDispatcher dispatcher,
                       WorkersProperties props, ObjectMapper json) {
        this.jobStore = jobStore;
        this.dispatcher = dispatcher;
        this.props = props;
        this.json = json;
        this.owner = "workers-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * One poll cycle. Also exposed as a public method so tests can drive
     * {@code poll()} manually without waiting for the scheduler.
     */
    @Scheduled(fixedDelayString = "${amrutam.workers.poll-interval-ms:5000}")
    public void poll() {
        Instant now = Instant.now();
        List<JobStore.DueJob> due = jobStore.dueJobs(now, props.getBatchSize());
        lastPollAt.set(now);
        for (JobStore.DueJob job : due) {
            process(job, now);
        }
        if (due.isEmpty()) {
            log.debug("No due jobs at {}", now);
        }
    }

    /**
     * @return the last time {@link #poll()} started a cycle (health indicator
     *         uses this to decide whether the processor is alive)
     */
    public Instant lastPollAt() {
        return lastPollAt.get();
    }

    private void process(JobStore.DueJob job, Instant now) {
        Instant leaseExpiry = now.plusSeconds(props.getJobLeaseSeconds());
        if (!jobStore.claim(job.id(), owner, leaseExpiry)) {
            // Someone else grabbed it first — skip silently.
            log.debug("Could not claim job {} — lease held by another worker", job.id());
            return;
        }

        int attempt = job.attempt() + 1; // claim() already bumped the row
        try {
            Map<String, Object> payload = parsePayload(job.payloadJson());
            dispatcher.find(job.type()).ifPresentOrElse(
                    handler -> {
                        handler.handle(job.id(), payload);
                        jobStore.complete(job.id());
                        log.info("Job {} type={} completed on attempt {}", job.id(), job.type(), attempt);
                    },
                    () -> {
                        // No handler: mark DONE to avoid infinite re-queueing;
                        // log loudly so an operator can investigate.
                        log.warn("No JobHandler registered for type {} — marking job {} DONE",
                                job.type(), job.id());
                        jobStore.complete(job.id());
                    });
        } catch (Exception e) {
            log.error("Job {} type={} failed on attempt {}: {}",
                    job.id(), job.type(), attempt, e.getMessage(), e);
            if (attempt > props.getMaxRetries()) {
                jobStore.deadLetter(job.id(), e.getMessage());
                log.error("Job {} dead-lettered after {} attempts: {}",
                        job.id(), attempt, e.getMessage());
            } else {
                Instant nextRun = nextRunAfterFailure(attempt, now);
                // The row's attempt column was already bumped to `attempt`
                // during claim(); we pass that value back so the CAS in
                // scheduleRetry() still matches the actual row state.
                jobStore.scheduleRetry(job.id(), attempt, nextRun);
            }
        }
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return Map.of();
        }
        try {
            return json.readValue(payloadJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            // A malformed payload is a permanent failure, not a retryable one.
            log.error("Unparseable payload for job: {}", e.getMessage());
            throw new IllegalStateException("Bad payload JSON: " + e.getMessage(), e);
        }
    }

    private Instant nextRunAfterFailure(int attempt, Instant now) {
        long base = props.getBackoffBaseSeconds();
        long cap = props.getBackoffMaxSeconds();
        long delaySeconds = Math.min(base * (1L << (attempt - 1)), cap);
        return now.plus(Duration.ofSeconds(delaySeconds));
    }
}
