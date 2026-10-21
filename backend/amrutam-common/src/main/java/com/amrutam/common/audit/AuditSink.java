package com.amrutam.common.audit;

/**
 * Pluggable sink for {@link AuditEvent}s. Implementations choose how events
 * are persisted (in-memory queue, JPA table, log line, ...).
 */
public interface AuditSink {

    /**
     * Records an event. Implementations should never throw for a bad event;
     * log-and-swallow is the expected behaviour.
     *
     * @param event the event to record
     */
    void record(AuditEvent event);
}
