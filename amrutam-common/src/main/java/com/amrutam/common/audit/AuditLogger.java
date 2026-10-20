package com.amrutam.common.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Log-based {@link AuditSink} that emits one JSON line per event through a
 * dedicated SLF4J logger named {@code "AUDIT"}.
 *
 * <p>This is the "fire and forget" sink; durable persistence (the
 * {@code audit_logs} table) is layered on top by the api module.
 */
public class AuditLogger implements AuditSink {

    /** The logger name that log aggregators should route to the audit store. */
    public static final String LOGGER_NAME = "AUDIT";

    private static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);

    private final ObjectMapper objectMapper;

    /**
     * Creates a logger using the provided mapper (typically Spring's).
     *
     * @param objectMapper the mapper used to serialize the event line
     */
    public AuditLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(AuditEvent event) {
        if (!log.isInfoEnabled()) {
            return;
        }
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("ts", Instant.now().toString());
        line.put("action", event.action());
        line.put("actorId", event.actorId() == null ? null : event.actorId().toString());
        line.put("ip", event.ip());
        line.put("details", event.details());
        try {
            log.info(objectMapper.writeValueAsString(line));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit event for action {}: {}",
                    event.action(), e.getMessage());
        }
    }
}
