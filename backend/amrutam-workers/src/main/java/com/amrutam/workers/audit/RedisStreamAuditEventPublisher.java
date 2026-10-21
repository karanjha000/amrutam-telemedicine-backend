package com.amrutam.workers.audit;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.amrutam.workers.config.WorkersProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * {@link AuditEventPublisher} that XADDs structured fields onto the
 * configured Redis Stream ({@code amrutam.audit-stream}, default
 * {@code amrutam:audit-events}).
 */
@Component
@ConditionalOnProperty(name = "amrutam.audit.redis-stream", havingValue = "true", matchIfMissing = true)
public class RedisStreamAuditEventPublisher implements AuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisStreamAuditEventPublisher.class);

    private final RedisTemplate<String, Object> redis;
    private final ObjectMapper json;
    private final String stream;

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
            byte[] streamKey = stream.getBytes(StandardCharsets.UTF_8);
            redis.execute((RedisConnection conn) -> {
                Map<byte[], byte[]> fields = new LinkedHashMap<>();
                fields.put(b("event_id"), b(eventId.toString()));
                fields.put(b("actor_id"), b(actorId == null ? "" : actorId.toString()));
                fields.put(b("action"), b(action == null ? "" : action));
                fields.put(b("target"), b(target == null ? "" : target));
                fields.put(b("ip"), b(ip == null ? "" : ip));
                fields.put(b("details"), b(detailsJson));
                fields.put(b("occurred_at"),
                        b(occurredAt == null ? Instant.now().toString() : occurredAt.toString()));
                MapRecord<byte[], byte[], byte[]> record =
                        MapRecord.create(streamKey, fields);
                return conn.streamCommands().xAdd(record);
            }, true);
            log.debug("Audit event {} (action={}) XADDed to {}", eventId, action, stream);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize audit event {} details: {}", eventId, e.getMessage());
        }
    }

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
