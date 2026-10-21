package com.amrutam.workers.jobs;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amrutam.common.idempotency.IdempotencyRepository;
import com.amrutam.workers.AmrutamWorkersApplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Drives {@link JobProcessor#poll()} three times manually against a handler
 * that fails on the first two attempts and succeeds on the third. The test
 * does NOT wait for the scheduler — it calls the public {@code poll()}
 * method directly so the run is deterministic.
 *
 * <p>Boots the real Spring context under the {@code test} profile:
 * {@link FlakyHandlerConfig} provides the H2 in-memory
 * {@link DataSource} (mirroring {@code application-test.yml}), a
 * {@code JdbcTemplate} that loads {@code schema-test.sql} explicitly (this
 * context does not run Spring SQL init), and a no-op
 * {@link IdempotencyRepository} stand-in so the shared
 * {@code CommonAutoConfiguration} can autowire without pulling in
 * amrutam-api. {@code application-test.yml} also disables the audit stream
 * so no Redis connection is needed, and sets backoff to 0 so the retried
 * job is due immediately. The flaky handler and its call counter are
 * contributed by {@link FlakyHandlerConfig}.</p>
 */
@SpringBootTest(classes = {AmrutamWorkersApplication.class, JobProcessorTest.FlakyHandlerConfig.class})
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
                        + "scheduled_at, next_run_at) VALUES (?, ?, 'PENDING', ?, 0, 5, now(), now())",
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

        /**
         * H2 in-memory datasource mirroring application-test.yml;
         * DB_CLOSE_DELAY=-1 keeps the named database alive for the JVM so
         * every consumer of the shared JdbcTemplate sees one database.
         */
        @Bean
        DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setUrl("jdbc:h2:mem:workers_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
                    + ";DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
            ds.setUsername("sa");
            ds.setPassword("");
            return ds;
        }

        /**
         * JdbcTemplate bound to the shared datasource; loads
         * schema-test.sql explicitly because this context does not run
         * Spring SQL init (same DDL the non-Spring JdbcTemplateJobStoreTest
         * uses).
         */
        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            var template = new JdbcTemplate(dataSource);
            try (var conn = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(conn, new ClassPathResource("schema-test.sql"));
            } catch (SQLException e) {
                throw new IllegalStateException("could not load schema-test.sql", e);
            }
            return template;
        }

        /**
         * No-op stand-in for the shared idempotency SPI — the api module
         * owns the JPA-backed implementation; this test only needs the bean
         * to satisfy CommonAutoConfiguration's autowiring.
         */
        @Bean
        IdempotencyRepository idempotencyRepository() {
            return new IdempotencyRepository() {
                @Override
                public void insertPending(UUID userId, String method, String path, String bodyHash) {
                }

                @Override
                public Optional<StoredKey> findActive(UUID userId, String method, String path, String bodyHash) {
                    return Optional.empty();
                }

                @Override
                public void markCompleted(UUID userId, String method, String path,
                        String bodyHash, String resultJson) {
                }

                @Override
                public void markFailed(UUID userId, String method, String path, String bodyHash) {
                }
            };
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
