package com.amrutam.workers.notify;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Declares {@link LoggingNotificationSink} as a {@code @Bean} rather than a
 * class-level {@code @Component} so the {@code @ConditionalOnMissingBean}
 * check sees the fully-built bean graph. Classpath scanning registers
 * annotated {@code @Component} beans in file order, so an annotation
 * on the class itself could race against the Redis sink's registration; a
 * plain {@code @Configuration @Bean} defers the decision until after all
 * component-scan candidates are known.
 */
@Configuration
public class NotifyConfig {

    /**
     * Default notification sink: SLF4J "NOTIFICATION" logger. Backed off
     * when any other {@link NotificationSink} is registered (e.g. the Redis
     * sink enabled via {@code amrutam.notifications.redis-sink=true}).
     *
     * @param json Jackson mapper for the event body
     * @return the logging sink
     */
    @Bean
    @ConditionalOnMissingBean(NotificationSink.class)
    public LoggingNotificationSink loggingNotificationSink(ObjectMapper json) {
        return new LoggingNotificationSink(json);
    }
}
