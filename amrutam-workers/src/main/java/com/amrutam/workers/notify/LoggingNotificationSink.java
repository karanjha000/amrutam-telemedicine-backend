package com.amrutam.workers.notify;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Default {@link NotificationSink}: logs to the dedicated {@code NOTIFICATION}
 * SLF4J logger (see the {@code logging.logger.NOTIFICATION} level in
 * {@code application.yml}) as a JSON line.
 *
 * <p>Registered only when no other {@link NotificationSink} bean is present
 * (e.g. the Redis sink, enabled with
 * {@code amrutam.notifications.redis-sink=true}).</p>
 *
 * <p>Declared as an inner {@code @Bean} of a plain {@code @Configuration}
 * (see {@code NotifyConfig}) rather than an annotated class so that
 * {@code @ConditionalOnMissingBean(NotificationSink.class)} is evaluated
 * against the fully-built bean graph instead of the current class-scanning
 * order (a documented Spring pitfall for same-package candidates).</p>
 */
public class LoggingNotificationSink implements NotificationSink {

    private static final Logger NOTIFICATION = LoggerFactory.getLogger("NOTIFICATION");
    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSink.class);

    private final ObjectMapper json;

    /**
     * @param json Jackson mapper to serialize the event
     */
    public LoggingNotificationSink(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public void send(UUID jobId, String channel, Map<String, Object> event) {
        try {
            String body = json.writeValueAsString(Map.of(
                    "jobId", jobId.toString(),
                    "channel", channel,
                    "event", event));
            NOTIFICATION.info("job={} channel={} {}", jobId, channel, body);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize notification for job {}: {}", jobId, e.getMessage());
        }
    }
}
