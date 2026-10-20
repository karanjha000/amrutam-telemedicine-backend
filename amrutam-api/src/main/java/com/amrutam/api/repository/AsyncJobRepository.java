package com.amrutam.api.repository;

import com.amrutam.api.entity.AsyncJob;
import com.amrutam.api.entity.JobStatus;
import com.amrutam.api.entity.SagaStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link AsyncJob}.
 *
 * <p><b>Worker-facing ownership note:</b> the api module owns this entity
 * and repository (including the {@code async_jobs} and {@code saga_steps}
 * tables). The amrutam-workers module does NOT redeclare these JPA types;
 * it enqueues work through the {@code AsyncBridge} SPI (api writes
 * {@code PENDING} rows) and performs its claim/mark-done/schedule-retry/
 * mark-dead transitions through the methods below, so there is a single
 * source of truth for the schema and no duplicate metamodel entries.</p>
 */
public interface AsyncJobRepository extends JpaRepository<AsyncJob, UUID> {

    /**
     * Job types currently enqueued by the api module.
     */
    final class JobType {
        /** Post-booking reminder notification. */
        public static final String CONSULTATION_SCHEDULED_NOTIFICATION = "consultation.scheduled-notification";

        private JobType() {
        }
    }

    /**
     * Enqueues a new PENDING job (the api side of {@code AsyncBridge}).
     *
     * @param userId owning user
     * @param type job type (see {@link JobType})
     * @param payloadJson serialized payload
     * @param scheduledAt intended run time
     * @param maxAttempt retry ceiling
     * @return the saved job
     */
    @Modifying
    @Transactional
    @Query("insert into AsyncJob(id, type, status, payload, attempt, maxAttempt, scheduledAt, lastRunAt, "
            + "nextRunAt, leaseOwner, leaseExpiry, createdAt, updatedAt) "
            + "values (:id, :type, 'PENDING', :payload, 0, :maxAttempt, :scheduledAt, null, null, "
            + "null, null, :scheduledAt, :scheduledAt)")
    AsyncJob enqueue(@Param("id") UUID id,
                     @Param("userId") UUID userId,
                     @Param("type") String type,
                     @Param("payload") String payloadJson,
                     @Param("scheduledAt") Instant scheduledAt,
                     @Param("maxAttempt") int maxAttempt);

    /**
     * Records a saga step row (compensation trace).
     *
     * @param sagaId saga
     * @param stepIndex step ordinal
     * @param stepName step name
     * @param status step status
     * @param detail optional detail
     * @return the saved step
     */
    @Modifying
    @Transactional
    @Query("insert into SagaStep(id, sagaId, stepIndex, stepName, status, detail, createdAt) "
            + "values (:id, :sagaId, :stepIndex, :stepName, :status, :detail, :now)")
    SagaStep recordSagaStep(@Param("id") UUID id,
                            @Param("sagaId") UUID sagaId,
                            @Param("stepIndex") int stepIndex,
                            @Param("stepName") String stepName,
                            @Param("status") String status,
                            @Param("detail") String detail,
                            @Param("now") Instant now);

    /**
     * Jobs a worker may claim right now: PENDING and due ({@code next_run_at
     * IS NULL} or {@code next_run_at <= :before}), oldest scheduled first.
     *
     * @param before claim horizon
     * @return due jobs
     */
    @Query("select j from AsyncJob j where j.status = 'PENDING' "
            + "and (j.nextRunAt is null or j.nextRunAt <= :before) "
            + "order by j.scheduledAt")
    List<AsyncJob> findDueJobs(@Param("before") Instant before);

    /**
     * Claims due jobs for a lease owner: PENDING -&gt; IN_PROGRESS, bumps
     * the attempt counter, sets the lease.
     *
     * @param before claim horizon
     * @param owner worker owner id
     * @param leaseExpiry when the lease lapses
     * @return rows claimed
     */
    @Modifying
    @Transactional
    @Query("update AsyncJob j set j.status = 'IN_PROGRESS', j.leaseOwner = :owner, "
            + "j.leaseExpiry = :leaseExpiry, j.attempt = j.attempt + 1, j.lastRunAt = now(), "
            + "j.updatedAt = now() where j.status = 'PENDING' "
            + "and (j.nextRunAt is null or j.nextRunAt <= :before)")
    int claimDueJobs(@Param("before") Instant before,
                     @Param("owner") String owner,
                     @Param("leaseExpiry") Instant leaseExpiry);

    /**
     * Marks a job DONE.
     *
     * @param id job
     * @return rows affected
     */
    @Modifying
    @Transactional
    @Query("update AsyncJob j set j.status = 'DONE', j.leaseOwner = null, j.leaseExpiry = null, "
            + "j.updatedAt = now() where j.id = :id")
    int markDone(@Param("id") UUID id);

    /**
     * Returns a failed job to PENDING with a backoff-rescheduled
     * {@code nextRunAt} (workers compute the backoff and pass it in).
     *
     * @param id job
     * @param nextRun when the next attempt may run
     * @return rows affected
     */
    @Modifying
    @Transactional
    @Query("update AsyncJob j set j.status = 'PENDING', j.nextRunAt = :nextRun, j.leaseOwner = null, "
            + "j.leaseExpiry = null, j.updatedAt = now() where j.id = :id")
    int scheduleRetry(@Param("id") UUID id, @Param("nextRun") Instant nextRun);

    /**
     * Dead-letters a job that exceeded its attempt ceiling.
     *
     * @param id job
     * @return rows affected
     */
    @Modifying
    @Transactional
    @Query("update AsyncJob j set j.status = 'DEAD', j.leaseOwner = null, j.leaseExpiry = null, "
            + "j.updatedAt = now() where j.id = :id")
    int markDead(@Param("id") UUID id);

    /**
     * Count of jobs in the given status (system-health analytics).
     *
     * @param status the status
     * @return count
     */
    long countByStatus(JobStatus status);
}
