package com.amrutam.api.audit;

import com.amrutam.api.entity.AuditLog;
import com.amrutam.api.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Durable audit recorder for the api module.
 *
 * <p>Writes one row into {@code audit_logs} for every mutating operation.
 * Callers invoke {@link #record} inside their own {@code @Transactional}
 * write so the audit row commits atomically with the business change (an
 * async / detached recorder would silently drop audit evidence when the
 * transaction rolls back — the wrong design for a compliance log).</p>
 *
 * <p>Also emits a JSON line on the {@code audit} SLF4J logger, which
 * {@code logback-spring.xml} routes to a dedicated rolling file so log
 * aggregators can ship the durable trail independently of the
 * {@code audit_logs} table.</p>
 */
@Component
public class AuditRecorder {

    /** The SLF4J logger name (lowercase {@code audit}) that logback routes to the audit file sink. */
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("audit");

    private static final Logger log = LoggerFactory.getLogger(AuditRecorder.class);

    private final AuditLogRepository auditLogs;
    private final ObjectMapper json;

    /**
     * @param auditLogs audit log repository
     * @param json json mapper for the {@code details} payload
     */
    public AuditRecorder(AuditLogRepository auditLogs, ObjectMapper json) {
        this.auditLogs = auditLogs;
        this.json = json;
    }

    /**
     * Persists a single audit row, committed together with the caller's
     * surrounding transaction.
     *
     * @param actorId acting user (null for system jobs)
     * @param action machine-readable action, e.g. {@code user.register}
     * @param target target resource, e.g. {@code user:42} (nullable)
     * @param ip client ip (nullable; callers capture it from the request)
     * @param details free-form key/value details (nullable)
     */
    public void record(UUID actorId, String action, String target, String ip, Map<String, Object> details) {
        AuditLog row = new AuditLog();
        row.setActorId(actorId);
        row.setAction(action);
        row.setTarget(target);
        row.setIp(ip);
        row.setDetails(details == null ? null : toJson(details));
        row.setOccurredAt(Instant.now());
        // The id is left null so JPA's @GeneratedValue fills it in.
        auditLogs.save(row);

        Map<String, Object> line = Map.of(
                "ts", row.getOccurredAt().toString(),
                "action", action,
                "actorId", actorId == null ? "" : actorId.toString(),
                "ip", ip == null ? "" : ip,
                "details", details == null ? Map.of() : details);
        try {
            AUDIT_LOG.info(json.writeValueAsString(line));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit log line for action {}: {}", action, e.getMessage());
        }
    }

    private String toJson(Map<String, Object> details) {
        try {
            return json.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit details; storing raw string instead", e);
            return String.valueOf(details);
        }
    }
}
