package com.amrutam.api.workerbridge;

import com.amrutam.api.entity.AsyncJob;
import com.amrutam.api.entity.JobStatus;
import com.amrutam.api.entity.SagaStep;
import com.amrutam.api.repository.AsyncJobRepository;
import com.amrutam.api.repository.SagaStepRepository;
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
 * Default {@link AsyncBridge} implementation: writes PENDING rows into the
 * {@code async_jobs} table (and saga-step rows into {@code saga_steps})
 * through the api-owned JPA repositories.
 *
 * <p>The workers module's {@code JobProcessor} later claims these rows via
 * its {@code JdbcTemplate} job store — the two halves meet at the shared
 * table, not at a shared Java class. This bridge is the <i>producer</i>
 * side of that seam; see {@code docs/ARCHITECTURE.md} for the full
 * saga/async boundary.</p>
 *
 * <p>Each write runs in {@code REQUIRES_NEW} so an enqueue is durable even
 * when the caller's surrounding transaction rolls back (booking-saga
 * compensation must not lose the fact that a notification was queued).</p>
 */
@Component
public class JdbcAsyncBridge implements AsyncBridge {

    /** Default retry ceiling for enqueued jobs. */
    public static final int DEFAULT_MAX_ATTEMPT = 5;

    private static final Logger log = LoggerFactory.getLogger(JdbcAsyncBridge.class);

    private final AsyncJobRepository jobs;
    private final SagaStepRepository sagaSteps;
    private final ObjectMapper json;

    /**
     * @param jobs async job repository
     * @param sagaSteps saga step repository
     * @param json json mapper for payload serialization
     */
    public JdbcAsyncBridge(AsyncJobRepository jobs,
                           SagaStepRepository sagaSteps,
                           ObjectMapper json) {
        this.jobs = jobs;
        this.sagaSteps = sagaSteps;
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
        AsyncJob job = new AsyncJob();
        job.setUserId(userId);
        job.setType(type);
        job.setStatus(JobStatus.PENDING);
        job.setPayload(payloadJson);
        job.setAttempt(0);
        job.setMaxAttempt(DEFAULT_MAX_ATTEMPT);
        job.setScheduledAt(now);
        // nextRunAt stays null -> "due immediately" (claim rule: next_run_at
        // IS NULL OR next_run_at <= now).
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        jobs.save(job);
        log.debug("Enqueued async job type={} userId={} job={}", type, userId, job.getId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSagaStep(UUID sagaId, int stepIndex, String stepName, String status, String detail) {
        SagaStep step = new SagaStep();
        step.setSagaId(sagaId);
        step.setStepIndex(stepIndex);
        step.setStepName(stepName);
        step.setStatus(status);
        step.setDetail(detail);
        step.setCreatedAt(Instant.now());
        sagaSteps.save(step);
        log.debug("Recorded saga step saga={} step={} name={} status={}", sagaId, stepIndex, stepName, status);
    }
}
