package com.amrutam.workers.audit;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Persists audit events into the shared {@code audit_logs} table via plain,
 * parameterized {@code JdbcTemplate} SQL — deliberately <b>not</b> through
 * amrutam-api's JPA {@code AuditLog} entity / repository (keeping this
 * module acyclic: workers depends only on common).
 *
 * <p>The INSERT is {@code ON CONFLICT (id) DO NOTHING} keyed on the event
 * id, so a redelivered Redis stream entry is a no-op and the worker is
 * safe to crash / restart mid-batch.</p>
 */
@Component
public class AuditLogWriter {

    private final JdbcTemplate jdbc;

    /**
     * @param jdbc shared-datasource JdbcTemplate
     */
    public AuditLogWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Idempotently persists one audit event (event id = row id).
     *
     * @param id         stream event id (the audit_logs PK)
     * @param actorId    acting user/doctor id
     * @param action     verb
     * @param target     target description
     * @param ip         client ip
     * @param details    JSON-encoded detail string
     * @param occurredAt event time
     * @return rows actually inserted (0 if the row already existed)
     */
    public int write(UUID id, UUID actorId, String action, String target, String ip,
                     String details, Instant occurredAt) {
        return jdbc.update(
                "INSERT INTO audit_logs (id, actor_id, action, target, ip, details, occurred_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING",
                id,
                actorId,
                action,
                target,
                ip,
                details,
                new Timestamp(occurredAt));
    }
}
