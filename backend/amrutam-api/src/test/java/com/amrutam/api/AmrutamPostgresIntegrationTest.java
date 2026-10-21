package com.amrutam.api;

import com.amrutam.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-PostgreSQL integration test.
 *
 * <p>SKIPPED unless {@code AMRUTAM_TEST_PG=true} is set in the environment —
 * CI runs the H2-backed tests by default and runs this class on
 * infrastructure that has Docker available.</p>
 */
@Testcontainers
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "AMRUTAM_TEST_PG", matches = "true")
class AmrutamPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("amrutam")
                    .withUsername("amrutam")
                    .withPassword("amrutam");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:schema.sql");
        registry.add("spring.sql.init.data-locations", () -> "classpath:data.sql");
        registry.add("amrutam.cache.redis-enabled", () -> "false");
        registry.add("spring.autoconfigure.exclude",
                () -> "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration");
    }

    @Autowired
    private UserRepository users;

    @Test
    void seededAdminExists() {
        assertThat(users.findByUsername("admin")).isPresent();
    }
}
