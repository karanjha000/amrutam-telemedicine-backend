package com.amrutam.workers.jobs;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * {@link JobStore} backed by raw, parameterized {@code JdbcTemplate} SQL.
 *
 * <p>Boundary: this class is the workers-side owner of its own DML against the
 * shared {@code async_jobs} table. Column names match
 * {@code amrutam-api}'s {@code schema.sql} {@code async_jobs} table
 * ({@code id uuid PK, type text, status text, payload text, attempt int,
 * max_attempt int, scheduled_at timestamptz, last_run_at timestamptz,
 * next_run_at timestamptz, lease_owner text, lease_expiry timestamptz,
 * created_at timestamptz, updated_at timestamptz}). If the api module ever
 * renames a column, this class is the place to break — keep the two in sync
 * by hand (no shared entity on purpose).</p>
 *
 * <p>The claim is a CAS: it only succeeds when the row is still PENDING and
 * either has no active lease or the lease has expired, so two workers polling
 * concurrently never double-run a job.</p>
 */
@Repository
public class JdbcTemplateJobStore implements JobStore {

    private static final RowMapper<DueJob> DUE_JOB_ROW_MAPPER =
            (rs, rowNum) -> new DueJob(
                    UUID.fromString(rs.getString("id")),
                    rs.getString("type"),
                    rs.getInt("attempt"),
                    rs.getString("payload"),
                    rs.getTimestamp("scheduled_at").toInstant());

    private final JdbcTemplate jdbc;

    /**
     * @param jdbc JdbcTemplate bound to the shared datasource
     */
    public JdbcTemplateJobStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<DueJob> dueJobs(Instant before, int batchSize) {
        return jdbc.query(
                "SELECT id, type, attempt, payload, scheduled_at FROM async_jobs "
                        + "WHERE status = 'PENDING' AND next_run_at <= ? "
                        + "ORDER BY scheduled_at LIMIT ?",
                DUE_JOB_ROW_MAPPER,
                java.sql.Timestamp.from(before), batchSize);
    }

    @Override
    public boolean claim(UUID id, String owner, Instant leaseExpiry) {
        int updated = jdbc.update(
                "UPDATE async_jobs SET status = 'IN_PROGRESS', lease_owner = ?, "
                        + "lease_expiry = ?, attempt = attempt + 1, last_run_at = now(), "
                        + "updated_at = now() "
                        + "WHERE id = ? AND status = 'PENDING' "
                        + "AND (lease_owner IS NULL OR lease_expiry < now())",
                owner, java.sql.Timestamp.from(leaseExpiry), id);
        return updated == 1;
    }

    @Override
    public void complete(UUID id) {
        jdbc.update(
                "UPDATE async_jobs SET status = 'DONE', lease_owner = NULL, "
                        + "lease_expiry = NULL, updated_at = now() WHERE id = ?",
                id);
    }

    @Override
    public void scheduleRetry(UUID id, int attempt, Instant nextRun) {
        jdbc.update(
                "UPDATE async_jobs SET status = 'PENDING', next_run_at = ?, "
                        + "lease_owner = NULL, lease_expiry = NULL, updated_at = now() "
                        + "WHERE id = ? AND attempt = ?",
                java.sql.Timestamp.from(nextRun), id, attempt);
    }

    /**
     * Records the failure on the row (audit-only — the schema has no
     * {@code error} column) and dead-letters it: status DEAD, lease released.
     * The {@code reason} is passed in only for log correlation in the caller.
     *
     * @param id     job id
     * @param reason log-only reason (not persisted)
     */
    @Override
    public void deadLetter(UUID id, String reason) {
        jdbc.update(
                "UPDATE async_jobs SET status = 'DEAD', lease_owner = NULL, "
                        + "lease_expiry = NULL, updated_at = now() WHERE id = ?",
                id);
    }
}
