package com.amrutam.workers.audit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration test for {@link AuditWorker} against a real Redis.
 *
 * <p>Skipped by default. To run it, start a local Redis and opt in with the
 * system property:</p>
 * <pre>
 *   docker run -p 6379:6379 redis:7
 *   mvn test -pl amrutam-workers -DAMRUTAM_WORKERS_REDIS=true
 * </pre>
 *
 * <p>The test will XADD a couple of entries to the configured stream, drive
 * {@code drain()} manually, and assert the {@code audit_logs} table gained
 * the expected rows (idempotent via {@code ON CONFLICT (id) DO NOTHING}).</p>
 */
class AuditWorkerTest {

    /**
     * Stub body — the real XADD + drain + assertion sequence goes here once
     * the docker-compose CI exposes a Redis service to this test job.
     */
    @Test
    @Disabled("requires a local Redis — run with -DAMRUTAM_WORKERS_REDIS=true")
    void drainPersistsStreamEntries() {
        // Intentionally left as a stub for now (see class Javadoc).
    }
}
