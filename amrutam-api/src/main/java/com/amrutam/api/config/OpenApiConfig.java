package com.amrutam.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc / OpenAPI configuration.
 *
 * <p>Defines two logical API groups (the public {@code /api/v1} surface and
 * the {@code /api/v1/admin} sub-tree) and a global JWT bearer security
 * scheme, so the swagger UI can use "Authorize" with a Bearer token.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "JWT Bearer";

    /**
     * Public API group.
     *
     * @return the grouped OpenAPI for {@code /api/v1/**}
     */
    @Bean
    public GroupedOpenApi apiV1Group() {
        return GroupedOpenApi.builder()
                .group("api-v1")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    /**
     * Admin API group.
     *
     * @return the grouped OpenAPI for {@code /api/v1/admin/**}
     */
    @Bean
    public GroupedOpenApi adminGroup() {
        return GroupedOpenApi.builder()
                .group("admin")
                .pathsToMatch("/api/v1/admin/**")
                .build();
    }

    /**
     * OpenAPI document with a JWT bearer security scheme and a global
     * security requirement, applied to all groups.
     *
     * @return the customized OpenAPI instance
     */
    @Bean
    public OpenAPI openAPI() {
        Components components = new Components()
                .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization"));
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(components);
    }

    // Kept for parity with the spec's "list of requirements"; unused directly.
    @SuppressWarnings("unused")
    private static List<SecurityRequirement> single(SecurityRequirement r) {
        return List.of(r);
    }
}
