package com.amrutam.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityAutoConfigurationTest {

    @Test
    void autoConfigurationImportsCustomSecurityConfig() throws IOException {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")) {
            assertTrue(in != null, "AutoConfiguration.imports file is missing");
            String imports = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(imports.contains("com.amrutam.common.security.SecurityConfig"),
                    "Custom security config must be auto-registered so default HTTP Basic auth is not used");
        }
    }
}
