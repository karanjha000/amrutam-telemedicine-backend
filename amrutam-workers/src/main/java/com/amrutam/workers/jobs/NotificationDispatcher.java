package com.amrutam.workers.jobs;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.amrutam.workers.config.WorkersProperties;

/**
 * Collects every registered {@link JobHandler} bean into a type-keyed map and
 * resolves handlers for incoming jobs.
 *
 * <p>Pattern-based lookup: a handler whose {@code type()} ends with
 * {@code -notification} acts as a catch-all for any job type that itself ends
 * with {@code -notification} (and is not covered by a more specific handler).
 * This keeps the dispatcher extensible without a registry entry per
 * notification subtype (e.g. {@code prescription.renewed-notification},
 * {@code consultation.reminder}…).</p>
 */
@Component
public class NotificationDispatcher {

    private final Map<String, JobHandler> handlerByType;
    private final List<JobHandler> allHandlers;
    private final WorkersProperties props;

    /**
     * @param handlers every JobHandler bean in the context
     * @param props    workers configuration (notification channel, etc.)
     */
    public NotificationDispatcher(List<JobHandler> handlers, WorkersProperties props) {
        this.allHandlers = handlers;
        this.handlerByType = handlers.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        h -> h.type(),
                        h -> h));
        this.props = props;
    }

    /**
     * Resolves the handler for a job type.
     *
     * @param type the async_jobs {@code type} value
     * @return the registered handler, or an empty Optional if none matches
     */
    public Optional<JobHandler> find(String type) {
        JobHandler exact = handlerByType.get(type);
        if (exact != null) {
            return Optional.of(exact);
        }
        // Fallback: any handler whose declared type is a wildcard
        // ("*-notification", the generic catch-all) covers any job type that
        // ends in "-notification" and was not matched exactly above.
        if (type != null && type.endsWith("-notification")) {
            return allHandlers.stream()
                    .filter(h -> h.type().endsWith("-notification"))
                    .findFirst();
        }
        return Optional.empty();
    }

    /**
     * @return the channel to publish notifications on
     */
    public String channel() {
        return props.getNotificationChannel();
    }
}
