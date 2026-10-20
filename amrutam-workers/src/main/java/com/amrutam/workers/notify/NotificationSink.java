package com.amrutam.workers.notify;

import java.util.Map;
import java.util.UUID;

/**
 * SPI for delivering a notification event to a consumer transport.
 *
 * <p>The default implementation is {@link LoggingNotificationSink}
 * (SLF4J "NOTIFICATION" logger — enough for dev). A Redis-pub/sub sink is
 * available behind the {@code amrutam.notifications.redis-sink=true}
 * property for deployments where a separate consumer (the frontend / a
 * websocket fan-out) listens on the notification channel.</p>
 */
public interface NotificationSink {

    /**
     * Delivers one notification event.
     *
     * @param jobId   originating async_jobs id (for correlation / idempotency
     *                on the receiving side)
     * @param channel target channel (from the job payload or the module
     *                default, {@code amrutam.workers.notification-channel})
     * @param event   the event body
     */
    void send(UUID jobId, String channel, Map<String, Object> event);
}
