package com.amrutam.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wires {@link ApiProperties} into the environment.
 *
 * <p>Schema initialization itself is driven by plain {@code spring.sql.init.*}
 * properties in {@code application.yml} ({@code spring.sql.init.mode:
 * ${AMRUTAM_SQL_INIT:always}}, with {@code schema.sql} / {@code data.sql} on
 * the classpath), so no custom {@code EnvironmentPostProcessor} is needed:
 * the {@code always} mode makes the bundled DDL idempotent-friendly
 * ({@code IF NOT EXISTS}) and safe to re-run on every boot.</p>
 */
@Configuration
@EnableConfigurationProperties(ApiProperties.class)
public class DataSourceConfig {
}
