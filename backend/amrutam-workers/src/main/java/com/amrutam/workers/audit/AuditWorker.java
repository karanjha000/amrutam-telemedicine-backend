package com.amrutam.workers.audit;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.ByteRecord;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.amrutam.workers.config.WorkersProperties;

/**
 * Drains the Redis audit-events stream into the {@code audit_logs} table
 * using a consumer group so multiple worker replicas split the backlog.
 */
@Component
@ConditionalOnProperty(name = "amrutam.audit.redis-stream", havingValue = "true", matchIfMissing = true)
public class AuditWorker {

    private static final Logger log = LoggerFactory.getLogger(AuditWorker.class);
    private static final String CONSUMER_GROUP = "workers-audit";
    private static final String CONSUMER_NAME = "worker-" + UUID.randomUUID().toString().substring(0, 8);
    private static final int BATCH = 100;

    private final RedisTemplate<String, Object> redis;
    private final AuditLogWriter writer;
    private final String stream;

    public AuditWorker(RedisTemplate<String, Object> redis,
                       AuditLogWriter writer,
                       WorkersProperties props) {
        this.redis = redis;
        this.writer = writer;
        this.stream = props.getAuditStream();
    }

    @Scheduled(fixedDelay = 2000)
    public void drain() {
        byte[] streamKey = stream.getBytes(StandardCharsets.UTF_8);
        Consumer consumer = Consumer.from(CONSUMER_GROUP, CONSUMER_NAME);
        // ">" = only entries not yet consumed by this consumer group
        StreamOffset<byte[]> offset = StreamOffset.create(streamKey, ReadOffset.from(">"));

        try {
            @SuppressWarnings("unchecked")
            StreamOffset<byte[]>[] offsets = (StreamOffset<byte[]>[]) new StreamOffset[] { offset };
            var raw = redis.execute((RedisConnection conn) -> {
                try {
                    conn.streamCommands().xGroupCreate(streamKey, CONSUMER_GROUP, ReadOffset.from("0"), false);
                } catch (Exception busy) {
                    // BUSYGROUP: group already exists
                }
                return conn.streamCommands().xReadGroup(
                        consumer,
                        StreamReadOptions.empty().count(BATCH),
                        offsets);
            }, true);
            if (raw != null) {
                for (ByteRecord record : raw) {
                    handleRecord(record, streamKey);
                }
            }
        } catch (Exception e) {
            log.error("Audit drain sweep failed: {}", e.getMessage(), e);
        }
    }

    private void handleRecord(ByteRecord record, byte[] streamKey) {
        String entryId = record.getId().toString();
        Map<String, String> fields = new HashMap<>();
        record.getValue().forEach((k, v) ->
                fields.put(new String(k, StandardCharsets.UTF_8),
                           new String(v, StandardCharsets.UTF_8)));
        try {
            UUID eventId = UUID.fromString(
                    fields.getOrDefault("event_id", UUID.randomUUID().toString()));
            String actorStr = fields.get("actor_id");
            UUID actorId = (actorStr == null || actorStr.isEmpty())
                    ? null : UUID.fromString(actorStr);
            String occStr = fields.get("occurred_at");
            Instant occurredAt = (occStr == null || occStr.isEmpty())
                    ? Instant.now() : Instant.parse(occStr);
            int inserted = writer.write(
                    eventId, actorId,
                    fields.get("action"), fields.get("target"), fields.get("ip"),
                    fields.get("details"), occurredAt);
            redis.execute((RedisConnection conn) ->
                    conn.streamCommands().xAck(streamKey, CONSUMER_GROUP, entryId),
                    true);
            if (inserted > 0) {
                log.info("Audit event {} persisted", entryId);
            }
        } catch (Exception ex) {
            log.error("Could not persist audit stream entry {}: {}", entryId, ex.getMessage());
        }
    }
}
