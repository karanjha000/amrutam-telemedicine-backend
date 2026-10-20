package com.amrutam.workers.jobs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * H2-backed integration test for {@link JdbcTemplateJobStore}.
 *
 * <p>Plain JDBC against the {@code schema-test.sql} DDL — no Spring context
 * needed, so it stays fast. Exercises the full claim/complete/retry/dead-letter
 * lifecycle as it would run against the shared {@code async_jobs} table.</p>
 */
class JdbcTemplateJobStoreTest {

    private static final String URL =
            "jdbc:h2:mem:jobstore_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                    + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";

    private static JdbcTemplate jdbc;
    private static JdbcTemplateJobStore store;
    private static Statement ddl;

    @BeforeAll
    static void setUp() throws Exception {
        DriverManagerDataSource ds = DriverManagerDataSource.builder()
                .url(URL).username("sa").build();
        jdbc = new JdbcTemplate(ds);
        // Load + run the H2 DDL.
        var schema = new String(
                JdbcTemplateJobStoreTest.class.getResourceAsStream("/schema-test.sql").readAllBytes());
        for (String stmt : schema.split(";")) {
            String s = stmt.strip();
            if (!s.isBlank() && !s.startsWith("--")) {
                jdbc.execute(s + ";");
            }
        }
        store = new JdbcTemplateJobStore(jdbc);
    }

    @AfterAll
    static void tearDown() {
        // DB_CLOSE_DELAY=-1 keeps the named in-memory DB alive for the JVM
        // lifetime; nothing to close here per-test.
    }

    @Test
    void lifecycle_pendingClaimDone() {
        UUID id = seedPendingJob();

        assertTrue(store.claim(id, "worker-1", Instant.now().plusSeconds(60)));
        // Second claim from another owner must no-op (row is IN_PROGRESS).
        assertFalse(store.claim(id, "worker-2", Instant.now().plusSeconds(60)));

        store.complete(id);
        assertEquals("DONE", statusOf(id));
    }

    @Test
    void lifecycle_scheduleRetryReleasesLease() {
        UUID id = seedPendingJob();
        assertTrue(store.claim(id, "worker-1", Instant.now().plusSeconds(60)));
        int attemptAfterClaim = attemptOf(id);
        assertEquals(1, attemptAfterClaim); // claim() bumps attempt

        store.scheduleRetry(id, attemptAfterClaim, Instant.now().plusSeconds(30));

        assertEquals("PENDING", statusOf(id));
        Integer withLease = jdbc.queryForObject(
                "SELECT COUNT(*) FROM async_jobs WHERE id = ? AND lease_owner IS NOT NULL",
                Integer.class, id);
        assertEquals(0, withLease);
    }

    @Test
    void lifecycle_deadLetter() {
        UUID id = seedPendingJob();
        store.deadLetter(id, "synthetic failure reason");
        assertEquals("DEAD", statusOf(id));
    }

    @Test
    void dueJobsReturnsOnlyPendingAndDue() {
        UUID pending = seedPendingJob();
        UUID done = seedPendingJob();
        store.complete(done);

        List<JobStore.DueJob> due = store.dueJobs(Instant.now(), 10);
        assertEquals(1, due.size());
        assertEquals(pending, due.get(0).id());
        assertNotNull(due.get(0).payloadJson());
    }

    private UUID seedPendingJob() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO async_jobs (id, type, status, payload, attempt, max_attempt, "
                        + "scheduled_at, next_run_at) VALUES (?, ?, 'PENDING', ?, 0, 5, now(), now())",
                id, "test-type", "{\"a\":1}");
        return id;
    }

    private String statusOf(UUID id) {
        return jdbc.queryForObject(
                "SELECT status FROM async_jobs WHERE id = ?", String.class, id);
    }

    private int attemptOf(UUID id) {
        return jdbc.queryForObject(
                "SELECT attempt FROM async_jobs WHERE id = ?", Integer.class, id);
    }
}
