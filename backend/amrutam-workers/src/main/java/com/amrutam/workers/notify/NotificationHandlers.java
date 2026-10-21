package com.amrutam.workers.notify;

import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.amrutam.workers.jobs.JobHandler;

/**
 * Concrete {@link JobHandler} types for notification jobs.
 *
 * <p>Both delegate to the active {@link NotificationSink} — logging by
 * default, Redis-pub/sub when {@code amrutam.notifications.redis-sink=true}.
 * The channel is taken from the job payload (key {@code channel}) and falls
 * back to the module default ({@code amrutam.workers.notification-channel}).</p>
 */
final class NotificationHandlers {

    /** The module-default channel when a payload omits its own. */
    private static final String DEFAULT_CHANNEL = "amrutam:notifications";

    private NotificationHandlers() {
    }

    /**
     * @return channel from the payload, or the module default
     */
    static String channelOf(Map<String, Object> payload) {
        Object c = payload.get("channel");
        return (c instanceof String s && !s.isBlank()) ? s : DEFAULT_CHANNEL;
    }

    /** Handler for a consultation-confirmation notification. */
    @Component
    @ConditionalOnProperty(name = "amrutam.workers.enabled", havingValue = "true", matchIfMissing = true)
    public static class NotificationHandler implements JobHandler {
        private final NotificationSink sink;
        private final com.amrutam.workers.config.WorkersProperties props;

        /**
         * @param sink active notification sink
         * @param props workers configuration
         */
        public NotificationHandler(NotificationSink sink,
                                   com.amrutam.workers.config.WorkersProperties props) {
            this.sink = sink;
            this.props = props;
        }

        @Override
        public String type() {
            return "consultation.confirmed-notification";
        }

        @Override
        public void handle(UUID jobId, Map<String, Object> payload) {
            String channel = channelOf(payload);
            sink.send(jobId, channel, payload);
        }
    }

    /**
     * Catch-all handler: any job type ending in {@code -notification} is
     * delivered through the same sink. This is how the dispatcher's pattern
     * lookup works — adding a new notification subtype (e.g.
     * {@code prescription.renewed-notification}) requires no new bean.
     */
    @Component
    @ConditionalOnProperty(name = "amrutam.workers.enabled", havingValue = "true", matchIfMissing = true)
    public static class GenericNotificationJobHandler implements JobHandler {
        private final NotificationSink sink;

        /**
         * @param sink active notification sink
         */
        public GenericNotificationJobHandler(NotificationSink sink) {
            this.sink = sink;
        }

        @Override
        public String type() {
            // The dispatcher keys handlers by their declared type() string; the
            // pattern fallback treats any type ending in "-notification" as
            // this handler's job.
            return "*-notification";
        }

        @Override
        public void handle(UUID jobId, Map<String, Object> payload) {
            sink.send(jobId, channelOf(payload), payload);
        }
    }
}
