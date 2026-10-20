package com.amrutam.workers;

import com.amrutam.workers.config.WorkersProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Amrutam async-workers process.
 *
 * <p>Runs the background pipeline that the API process does not: polling
 * {@code async_jobs} and dispatching to {@code JobHandler}s, reconciling the
 * booking saga (compensating stuck {@code PENDING_PAYMENT} consultations),
 * fanning out notification jobs through a {@code NotificationSink} SPI, and
 * draining the Redis audit-event stream into {@code audit_logs}.</p>
 *
 * <p>Dependency boundary: this module depends on {@code amrutam-common} only.
 * It owns no JPA entities and no Spring Data repositories — those belong to
 * {@code amrutam-api}. Instead every cross-module table is accessed with
 * plain, parameterized {@code JdbcTemplate} SQL so the two processes can share
 * one database (and one Redis) without a compile-time cycle
 * ({@code api -> workers}, not the other way).</p>
 *
 * <p>In docker-compose this process is selected with
 * {@code AMRUTAM_ROLE=workers} on the same image; see
 * {@code docs/ARCHITECTURE.md}.</p>
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(WorkersProperties.class)
public class AmrutamWorkersApplication {

    /**
     * Boots the workers application.
     *
     * @param args standard program arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(AmrutamWorkersApplication.class, args);
    }
}
