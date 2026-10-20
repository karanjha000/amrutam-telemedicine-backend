package com.amrutam.api.repository;

import com.amrutam.api.entity.AsyncJob;
import com.amrutam.api.entity.JobStatus;
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
 * and the JPA view of the {@code async_jobs} / {@code saga_steps} tables.
 * The amrutam-workers module owns <i>none</i> of these JPA types: it
 * enqueues work through the {@code AsyncBridge} SPI (the api writes
 * PENDING rows here) and performs its claim / mark-done / schedule-retry /
 * dead-letter transitions through a plain {@code JdbcTemplate} job store
 * against the same table. The dependency graph stays acyclic
 * ({@code api -> workers}, never the reverse), and both processes share one
 * source of truth for the schema. This repository therefore only contains
 * the SELECT/INSERT paths the api itself runs; no {@code @Modifying}
 * status-transition methods, so the api and the workers' JdbcTemplate SQL
 * cannot drift out of sync under JPA's flush timing.</p>
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
     * Jobs a worker may claim right now: PENDING and due
     * ({@code next_run_at IS NULL} or {@code next_run_at <= :before}),
     * oldest scheduled first.
     *
     * @param before claim horizon
     * @return due jobs
     */
    @Query("select j from AsyncJob j where j.status = 'PENDING' "
            + "and (j.nextRunAt is null or j.nextRunAt <= :before) "
            + "order by j.scheduledAt")
    List<AsyncJob> findDueJobs(@Param("before") Instant before);

    /**
     * Enqueues a new PENDING job (the api-side write path of
     * {@code AsyncBridge}).
     *
     * @param id surrogate key
     * @param userId owning user (scopes the job; may be null for system jobs)
     * @param type job type (see {@link JobType})
     * @param payloadJson serialized payload
     * @param scheduledAt intended run time
     * @param maxAttempt retry ceiling
     * @return the saved job
     */
    @Modifying
    @Transactional
    @Query("insert into AsyncJob(id, userId, type, status, payload, attempt, maxAttempt, scheduledAt, "
            + "lastRunAt, nextRunAt, leaseOwner, leaseExpiry, createdAt, updatedAt) "
            + "values (:id, :userId, :type, 'PENDING', :payload, 0, :maxAttempt, :scheduledAt, "
            + "null, null, null, null, :scheduledAt, :scheduledAt)")
    AsyncJob enqueue(@Param("id") UUID id,
                     @Param("userId") UUID userId,
                     @Param("type") String type,
                     @Param("payload") String payloadJson,
                     @Param("scheduledAt") Instant scheduledAt,
                     @Param("maxAttempt") int maxAttempt);

    /**
     * Inserts a saga-step row (compensation trace) via a JPQL insert
     * against the {@code saga_steps} table owned by the api module.
     *
     * @param id surrogate key
     * @param sagaId the saga
     * @param stepIndex step ordinal
     * @param stepName step name
     * @param status step status
     * @param detail optional detail
     */
    @Modifying
    @Transactional
    @Query("insert into SagaStep(id, sagaId, stepIndex, stepName, status, detail, createdAt) "
            + "values (:id, :sagaId, :stepIndex, :stepName, :status, :detail, :now)")
    void recordSagaStep(@Param("id") UUID id,
                         @Param("sagaId") UUID sagaId,
                         @Param("stepIndex") int stepIndex,
                         @Param("stepName") String stepName,
                         @Param("status") String status,
                         @Param("detail") String detail,
                         @Param("now") Instant now);

    /**
     * Counts jobs in a status (system-health analytics).
     *
     * @param status the status
     * @return the count
     */
    long countByStatus(JobStatus status);
}
