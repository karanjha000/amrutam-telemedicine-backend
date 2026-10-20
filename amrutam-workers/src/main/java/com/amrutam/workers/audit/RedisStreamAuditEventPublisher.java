package com.amrutam.workers.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.amrutam.workers.config.WorkersProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link AuditEventPublisher} that XADDs structured fields onto the
 * configured Redis Stream ({@code amrutam.audit-stream}, default
 * {@code amrutam:audit-events}).
 *
 * <p>Enabled by default (matches missing), so processes that share the
 * workers' Redis by default can publish; deployments that want only the
 * direct DB write set {@code amrutam.audit.redis-stream=false}. The worker
 * that drains the stream is {@code AuditWorker} in this same package.</p>
 */
@Component
@ConditionalOnProperty(name = "amrutam.audit.redis-stream", havingValue = "true", matchIfMissing = true)
public class RedisStreamAuditEventPublisher implements AuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamAuditEventPublisher.class);

    private final RedisTemplate<String, Object> redis;
    private final ObjectMapper json;
    private final String stream;

    /**
     * @param redis auto-configured RedisTemplate
     * @param json  Jackson mapper for the details JSON
     * @param props workers configuration (stream name)
     */
    public RedisStreamAuditEventPublisher(RedisTemplate<String, Object> redis,
                                          ObjectMapper json,
                                          WorkersProperties props) {
        this.redis = redis;
        this.json = json;
        this.stream = props.getAuditStream();
    }

    @Override
    public void publish(UUID actorId, String action, String target, String ip,
                        Map<String, String> details, Instant occurredAt) {
        UUID eventId = UUID.randomUUID();
        try {
            String detailsJson = json.writeValueAsString(details == null ? Map.of() : details);
            redis.execute((RedisConnection conn) -> {
                var xadd = conn.streamCommands().xAdd(
                        stream,
                        Map.of(
                                "event_id", eventId.toString(),
                                "actor_id", actorId == null ? "" : actorId.toString(),
                                "action", action == null ? "" : action,
                                "target", target == null ? "" : target,
                                "ip", ip == null ? "" : ip,
                                "details", detailsJson,
                                "occurred_at", occurredAt == null
                                        ? Instant.now().toString() : occurredAt.toString()));
                return xadd;
            }, true);
            log.debug("Audit event {} (action={}) XADDed to {}", eventId, action, stream);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize audit event {} details: {}", eventId, e.getMessage());
        }
    }
}
