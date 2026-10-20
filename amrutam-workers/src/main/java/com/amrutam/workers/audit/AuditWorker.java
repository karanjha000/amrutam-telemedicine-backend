package com.amrutam.workers.audit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.amrutam.workers.config.WorkersProperties;

/**
 * Drains the Redis audit-events stream into the {@code audit_logs} table.
 *
 * <p>Uses a consumer group ({@code workers-audit}) so multiple worker
 * replicas split the backlog without duplicate-delivering an entry more
 * than once in steady state; combined with the {@code ON CONFLICT (id) DO
 * NOTHING} in {@link AuditLogWriter}, even a redelivered entry after a
 * crash is a no-op.</p>
 *
 * <p>The XREADGROUP is bounded (latest {@code BATCH} pending entries); each
 * entry is ACKed after a successful INSERT. A failed INSERT leaves the
 * entry pending and the next sweep retries it. Stream entries are NOT
 * trimmed here (an operator-visible retention policy is out of scope); the
 * consumer-group cursor is what keeps the backlog bounded.</p>
 */
@Component
@ConditionalOnProperty(name = "amrutam.audit.redis-stream", havingValue = "true", matchIfMissing = true)
public class AuditWorker {

    private static final Logger log = LoggerFactory.getLogger(AuditWorker.class);
    private static final String CONSUMER_GROUP = "workers-audit";
    private static final String CONSUMER = "worker-" + UUID.randomUUID().toString().substring(0, 8);
    private static final int BATCH = 100;

    private final RedisTemplate<String, Object> redis;
    private final AuditLogWriter writer;
    private final String stream;

    /**
     * @param redis  auto-configured RedisTemplate
     * @param writer audit_log INSERT
     * @param props  workers configuration (stream name)
     */
    public AuditWorker(RedisTemplate<String, Object> redis,
                       AuditLogWriter writer,
                       com.amrutam.workers.config.WorkersProperties props) {
        this.redis = redis;
        this.writer = writer;
        this.stream = props.getAuditStream();
    }

    /**
     * One drain cycle. Also public so tests can drive it manually.
     */
    @Scheduled(fixedDelay = 2000)
    public void drain() {
        List<Map.Entry<String, Map<String, String>>> batch;
        try {
            batch = redis.execute((RedisConnection conn) -> {
                // Make sure the consumer group exists on first run.
                try {
                    conn.streamCommands().xGroupCreate(
                            stream, CONSUMER_GROUP, "0", false);
                } catch (Exception busyGroup) {
                    // BUSYGROUP: already created — that's fine.
                }
                var raw = conn.streamCommands().xReadGroup(
                        CONSUMER_GROUP, CONSUMER,
                        Map.of(stream, ">"),
                        BATCH);
                return raw.isEmpty() ? List.<Map.Entry<String, Map<String, String>>>of()
                        : toEntries(raw);
            }, true);
        } catch (Exception e) {
            log.error("Audit drain sweep failed: {}", e.getMessage(), e);
            return;
        }

        for (Map.Entry<String, Map<String, String>> entry : batch) {
            String entryId = entry.getKey();
            Map<String, String> fields = entry.getValue();
            try {
                UUID eventId = UUID.fromString(fields.getOrDefault("event_id", UUID.randomUUID().toString()));
                UUID actorId = fields.get("actor_id") == null
                        ? null : UUID.fromString(fields.get("actor_id"));
                Instant occurredAt = fields.get("occurred_at") == null
                        ? Instant.now()
                        : Instant.parse(fields.get("occurred_at"));
                int inserted = writer.write(
                        eventId,
                        actorId,
                        fields.get("action"),
                        fields.get("target"),
                        fields.get("ip"),
                        fields.get("details"),
                        occurredAt);
                // ACK so Redis stops redelivering this entry.
                redis.execute((RedisConnection conn) -> {
                    conn.streamCommands().xAck(stream, CONSUMER_GROUP, entryId);
                    return 0;
                }, true);
                if (inserted > 0) {
                    log.info("Audit event {} persisted ({})", entryId, "new row");
                }
            } catch (Exception e) {
                log.error("Could not persist audit stream entry {} (fields {}): {}",
                        entryId, fields.keySet(), e.getMessage());
                // Leave it pending — next sweep will retry.
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Map.Entry<String, Map<String, String>>> toEntries(Object raw) {
        List<Map.Entry<String, Map<String, String>>> out = new ArrayList<>();
        // raw: List[Map[StreamId, List<[id, Map<field, value>>]]]
        java.util.List<Object> outer = (java.util.List<Object>) raw;
        for (Object m : outer) {
            java.util.Map<Object, Object> byStream = (java.util.Map<Object, Object>) m;
            for (Object values : byStream.values()) {
                java.util.List<Object> entries = (java.util.List<Object>) values;
                for (Object e : entries) {
                    java.util.List<Object> pair = (java.util.List<Object>) e;
                    String id = String.valueOf(pair.get(0));
                    Map<String, String> fields = new HashMap<>();
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> rawFields = (Map<Object, Object>) pair.get(1);
                    rawFields.forEach((k, v) -> fields.put(String.valueOf(k), String.valueOf(v)));
                    out.add(Map.entry(id, fields));
                }
            }
        }
        return out;
    }
}
