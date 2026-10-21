package com.amrutam.workers.jobs;

import java.util.Map;
import java.util.UUID;

/**
 * A single async-job execution unit, selected by job type.
 *
 * <p>Implementations are Spring-collected by {@code NotificationDispatcher} /
 * {@code JobProcessor} and keyed by {@link #type()}. Throwing a runtime
 * exception from {@link #handle} signals a retry (exponential backoff); the
 * job processor decides when a job exhausts its retries and dead-letters it.
 * Job payloads are shared, so keep handlers idempotent with respect to the
 * {@code jobId}.</p>
 */
public interface JobHandler {

    /**
     * @return the async_jobs {@code type} this handler executes
     *         (e.g. {@code consultation.confirmed-notification})
     */
    String type();

    /**
     * Runs the business logic for one job.
     *
     * @param jobId   the originating row in {@code async_jobs}
     * @param payload deserialized JSON payload (already shared as a mutable
     *                map; handlers that mutate it do so at their own risk)
     */
    void handle(UUID jobId, Map<String, Object> payload);
}
