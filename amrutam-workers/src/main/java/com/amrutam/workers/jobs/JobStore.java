package com.amrutam.workers.jobs;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Abstraction over the shared {@code async_jobs} table for the workers
 * process.
 *
 * <p>The production implementation ({@code JdbcTemplateJobStore}) is plain
 * parameterized JDBC. This is deliberate: {@code async_jobs} (and its
 * {@code AsyncJobRepository}) is owned by {@code amrutam-api}, and workers
 * must not depend on the api module — otherwise the dependency graph becomes
 * a cycle. The workers side therefore owns its own SQL against the same
 * table. The column set must keep in sync with api's {@code schema.sql}
 * ({@code id, type, status, payload, attempt, max_attempt, scheduled_at,
 * last_run_at, next_run_at, lease_owner, lease_expiry, created_at,
 * updated_at}).</p>
 */
public interface JobStore {

    /**
     * One due job ready to be claimed.
     *
     * @param id           row id
     * @param type         handler type key
     * @param attempt      attempt count so far (0 = never run)
     * @param payloadJson  raw JSON payload (may be null)
     * @param scheduledAt  original scheduled time
     */
    record DueJob(UUID id, String type, int attempt, String payloadJson, Instant scheduledAt) {
    }

    /**
     * Selects up to {@code batchSize} jobs that are PENDING and due
     * (i.e. {@code next_run_at <= before}, ordered by {@code scheduled_at}).
     *
     * @param before      due-time cutoff
     * @param batchSize   maximum rows to return
     * @return due jobs (possibly empty, never null)
     */
    List<DueJob> dueJobs(Instant before, int batchSize);

    /**
     * Atomically claims a job: CAS from PENDING to IN_PROGRESS for this
     * owner. Safe to call against jobs another worker already claimed —
     * the UPDATE simply no-ops.
     *
     * @param id          job id
     * @param owner       worker instance identifier (host/pid)
     * @param leaseExpiry lease expiry; after this another worker may steal
     * @return {@code true} iff this call performed the claim
     */
    boolean claim(UUID id, String owner, Instant leaseExpiry);

    /**
     * Marks a job DONE and releases the lease.
     *
     * @param id job id
     */
    void complete(UUID id);

    /**
     * Reschedules a failed job for retry (exponential backoff computed by
     * the caller, in seconds).
     *
     * @param id       job id
     * @param attempt  the attempt that just failed (recorded for observability)
     * @param nextRun  when the job becomes due again
     */
    void scheduleRetry(UUID id, int attempt, Instant nextRun);

    /**
     * Dead-letters a job: status DEAD, lease released. The reason is logged,
     * not persisted — the shared schema has no dedicated error column, so
     * the operators' source of truth for "why is this dead" is the log line
     * written by {@code JobProcessor} before calling this.
     *
     * @param id     job id
     * @param reason human-readable reason (for log correlation only)
     */
    void deadLetter(UUID id, String reason);
}
