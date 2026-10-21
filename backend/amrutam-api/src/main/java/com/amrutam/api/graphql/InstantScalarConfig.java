package com.amrutam.api.graphql;

import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * Registers the custom {@code Instant} GraphQL scalar with the runtime.
 *
 * <p>Spring GraphQL requires custom scalars to be included in the runtime
 * wiring rather than only being present in the schema document. The scalar is
 * therefore registered both as a bean and as a runtime-wired type.</p>
 */
@Configuration
public class InstantScalarConfig {

    @Bean
    public GraphQLScalarType instantScalar() {
        Coercing<java.time.Instant, String> coercion = new InstantCoercion();
        return GraphQLScalarType.newScalar()
                .name("Instant")
                .description("An instant in time, ISO-8601 on the wire.")
                .coercing(coercion)
                .build();
    }

    @Bean
    public RuntimeWiringConfigurer instantWiringConfigurer(GraphQLScalarType instantScalar) {
        return builder -> builder.scalar(instantScalar);
    }
}
