package com.amrutam.workers.notify;

import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.amrutam.workers.config.WorkersProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis pub/sub {@link NotificationSink} for deployments where a separate
 * process (the frontend's websocket relay or a mobile-push fan-out) listens
 * on the notification channel.
 *
 * <p>Disabled by default; enabled with
 * {@code amrutam.notifications.redis-sink=true}. When enabled it
 * <i>replaces</i> {@link LoggingNotificationSink} (that sink is
 * {@code @ConditionalOnMissingBean}).</p>
 */
@Component
@ConditionalOnProperty(name = "amrutam.notifications.redis-sink", havingValue = "true")
public class RedisNotificationSink implements NotificationSink {

    private static final Logger log = LoggerFactory.getLogger(RedisNotificationSink.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final WorkersProperties props;

    /**
     * @param redis  auto-configured StringRedisTemplate (JSON-serialized body)
     * @param json   Jackson mapper
     * @param props  workers configuration (default channel)
     */
    public RedisNotificationSink(StringRedisTemplate redis,
                                 ObjectMapper json,
                                 WorkersProperties props) {
        this.redis = redis;
        this.json = json;
        this.props = props;
    }

    @Override
    public void send(UUID jobId, String channel, Map<String, Object> event) {
        String target = (channel == null || channel.isBlank())
                ? props.getNotificationChannel()
                : channel;
        try {
            String body = json.writeValueAsString(Map.of(
                    "jobId", jobId.toString(),
                    "event", event));
            // PUBLISH on the channel — any Redis consumer attached to it picks
            // it up (frontend websocket relay, push service, …).
            redis.convertAndSend(target, body);
            log.debug("Published notification job={} to channel {}", jobId, target);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification for job {}: {}", jobId, e.getMessage());
        }
    }
}
