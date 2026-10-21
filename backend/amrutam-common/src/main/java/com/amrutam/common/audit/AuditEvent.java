package com.amrutam.common.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A single auditable action. Implementations describe WHAT happened, WHO did
 * it, from WHERE and WHEN, plus free-form key/value details.
 */
public interface AuditEvent {

    /**
     * Machine-readable action (e.g. {@code appointment.created}).
     *
     * @return the action
     */
    String action();

    /**
     * The acting user; null when the action was performed by a system job.
     *
     * @return the actor id or null
     */
    UUID actorId();

    /**
     * The client IP.
     *
     * @return the IP address, or null when unknown
     */
    String ip();

    /**
     * When the action happened.
     *
     * @return the timestamp
     */
    Instant occurredAt();

    /**
     * Free-form details; empty map is valid.
     *
     * @return the details
     */
    Map<String, String> details();
}
