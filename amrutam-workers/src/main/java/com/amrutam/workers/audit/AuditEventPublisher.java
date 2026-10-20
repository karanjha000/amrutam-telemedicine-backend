package com.amrutam.workers.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes a structured audit event onto the Redis audit-event stream so
 * that a consuming process (the workers audit worker, or a SIEM) can persist
 * it out-of-band.
 *
 * <p>Both the interface and the default {@code RedisStream} implementation
 * live in this module so that the dependency graph stays acyclic:
 * amrutam-api owns the direct DB writer ({@code AuditLogWriter} there); this
 * publisher is for deployments where api and workers run as separate
 * processes sharing one Redis (api enqueues, workers drains). An optional
 * api dependency on workers can call this; the api's own default path never
 * touches it.</p>
 */
public interface AuditEventPublisher {

    /**
     * Enqueues one audit event.
     *
     * @param actorId    the acting user/doctor id
     * @param action     verb (e.g. {@code consultation.create})
     * @param target     target description
     * @param ip         client ip
     * @param details    structured detail map (JSON-serialized into the
     *                   stream entry)
     * @param occurredAt event time
     */
    void publish(UUID actorId, String action, String target, String ip,
                 Map<String, String> details, Instant occurredAt);
}
