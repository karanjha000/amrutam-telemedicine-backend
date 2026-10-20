package com.amrutam.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Amrutam telemedicine API.
 *
 * <p>Boots the REST + GraphQL surface, JPA persistence (PostgreSQL), Redis
 * caching, security (JWT via the shared {@code amrutam-common} kernel), and
 * Prometheus metrics. Scheduling is enabled so time-boxed background sweeps
 * (slot-lock expiry, idempotency GC) can run in-process in development.</p>
 */
@SpringBootApplication
@EnableScheduling
public class AmrutamApiApplication {

    /**
     * Boots the application.
     *
     * @param args standard program arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(AmrutamApiApplication.class, args);
    }
}
