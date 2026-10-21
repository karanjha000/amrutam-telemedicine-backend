package com.amrutam.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API-module-specific configuration, bound from the {@code amrutam.api.*}
 * property namespace.
 */
@ConfigurationProperties(prefix = "amrutam.api")
public class ApiProperties {

    /**
     * How the API module participates in schema initialization:
     * <ul>
     *   <li>{@code always} – run {@code spring.sql.init} on every boot
     *       (idempotent DDL; used in dev / docker-compose)</li>
     *   <li>{@code embedded} – only run init scripts when the bundled
     *       database is embedded (the Spring Boot default)</li>
     *   <li>{@code never} – never run init scripts
     *       (prod relies on a managed migration path, future module)</li>
     * </ul>
     */
    private String schemaInitMode = "embedded";

    /**
     * Optional explicit Redis connection URL (host:port) override.
     */
    private String redisUrl;

    /**
     * @return schema init mode
     */
    public String getSchemaInitMode() {
        return schemaInitMode;
    }

    /**
     * @param schemaInitMode the schema init mode
     */
    public void setSchemaInitMode(String schemaInitMode) {
        this.schemaInitMode = schemaInitMode;
    }

    /**
     * @return optional redis url
     */
    public String getRedisUrl() {
        return redisUrl;
    }

    /**
     * @param redisUrl the redis url
     */
    public void setRedisUrl(String redisUrl) {
        this.redisUrl = redisUrl;
    }
}
