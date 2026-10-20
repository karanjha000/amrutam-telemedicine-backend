package com.amrutam.api.workerbridge;

import com.amrutam.api.repository.AsyncJobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Default {@link AsyncBridge} implementation backed by JPA writes to the
 * {@code async_jobs} table (this is also where the owning {@code userId}
 * column on {@code async_jobs} is populated).
 *
 * <p>Jobs are scheduled to run immediately ({@code scheduledAt = now,
 * nextRunAt = null}) and left {@code PENDING}; the workers module's claim
 * loop ({@code findDueJobs} / {@code claimDueJobs}) picks them up under a
 * lease. Each write runs in its own transaction ({@code REQUIRES_NEW}) so
 * the enqueue is durable even when the caller's transaction rolls back.</p>
 */
@Component
public class JdbcAsyncBridge implements AsyncBridge {

    /** Default retry ceiling for enqueued jobs. */
    public static final int DEFAULT_MAX_ATTEMPT = 5;

    private static final Logger log = LoggerFactory.getLogger(JdbcAsyncBridge.class);

    private final AsyncJobRepository jobs;
    private final ObjectMapper json;

    /**
     * @param jobs JPA repository for async jobs
     * @param json json mapper for payload serialization
     */
    public JdbcAsyncBridge(AsyncJobRepository jobs, ObjectMapper json) {
        this.jobs = jobs;
        this.json = json;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enqueueAsyncJob(UUID userId, String type, Map<String, Object> payload) {
        String payloadJson;
        try {
            payloadJson = json.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize async job payload: " + type, e);
        }
        Instant now = Instant.now();
        jobs.enqueue(UUID.randomUUID(), userId, type, payloadJson, now, DEFAULT_MAX_ATTEMPT);
        log.debug("Enqueued async job type={} userId={}", type, userId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSagaStep(UUID sagaId, int stepIndex, String stepName, String status, String detail) {
        jobs.recordSagaStep(UUID.randomUUID(), sagaId, stepIndex, stepName, status, detail, Instant.now());
        log.debug("Recorded saga step saga={} step={} name={} status={}", sagaId, stepIndex, stepName, status);
    }
}
