package com.amrutam.workers.jobs;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Drives {@link JobProcessor#poll()} three times manually against a handler
 * that fails on the first two attempts and succeeds on the third. The test
 * does NOT wait for the scheduler — it calls the public {@code poll()}
 * method directly so the run is deterministic.
 *
 * <p>{@code application-test.yml} sets backoff = 0 so retried jobs are due
 * immediately, and disables the audit Redis stream so no Redis connection is
 * required to boot the context.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.yml")
class JobProcessorTest {

    @Autowired JdbcTemplate jdbc;

    @Autowired JobProcessor processor;

    @Autowired FlakyHandlerConfig.Calls counter;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM async_jobs");
        counter.calls.set(0);
    }

    @Test
    void jobFailsTwiceThenSucceedsOnThirdAttempt() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO async_jobs (id, type, status, payload, attempt, max_attempt, "
                        + "scheduled_at, next_run_at) VALUES (?, ?, 'PENDING', ?, 0, 5, '2026-01-01 00:00:00', '2026-01-01 00:00:00')",
                id, "test.retry", "{\"n\":1}");

        processor.poll(); // 1st attempt -> fail, scheduled for retry
        assertEquals("PENDING", statusOf(id));
        assertEquals(1, attemptOf(id));
        assertEquals(1, counter.calls.get());

        processor.poll(); // 2nd attempt -> fail, scheduled for retry
        assertEquals("PENDING", statusOf(id));
        assertEquals(2, attemptOf(id));
        assertEquals(2, counter.calls.get());

        processor.poll(); // 3rd attempt -> success, marked DONE
        assertEquals("DONE", statusOf(id));
        assertEquals(3, attemptOf(id));
        assertEquals(3, counter.calls.get());
    }

    private String statusOf(UUID id) {
        return jdbc.queryForObject("SELECT status FROM async_jobs WHERE id = ?", String.class, id);
    }

    private int attemptOf(UUID id) {
        return jdbc.queryForObject("SELECT attempt FROM async_jobs WHERE id = ?", Integer.class, id);
    }

    /**
     * A {@link JobHandler} that fails on the first two calls and succeeds on
     * the third. The counter is shared so the test can assert the exact
     * call sequence.
     */
    @Configuration
    static class FlakyHandlerConfig {

        /** Shared call counter, autowired into the test for assertions. */
        public static class Calls {
            final AtomicInteger calls = new AtomicInteger(0);
        }

        @Bean
        Calls callsCounter() {
            return new Calls();
        }

        @Bean
        JobHandler testRetryHandler(Calls counter) {
            return new JobHandler() {
                @Override
                public String type() {
                    return "test.retry";
                }

                @Override
                public void handle(UUID jobId, Map<String, Object> payload) {
                    int n = counter.calls.incrementAndGet();
                    if (n <= 2) {
                        throw new IllegalStateException("synthetic failure " + n);
                    }
                    // 3rd call succeeds silently.
                }
            };
        }
    }
}
