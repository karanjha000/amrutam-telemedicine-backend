package com.amrutam.api.graphql;

import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the custom {@code Instant} GraphQL scalar with the runtime.
 *
 * <p>Spring GraphQL picks up any {@link GraphQLScalarType} bean and merges it
 * into the schema, making {@code scalar Instant} available to resolvers.</p>
 */
@Configuration
public class InstantScalarConfig {

    /**
     * The {@code Instant} scalar: ISO-8601 strings on the wire,
     * {@link java.time.Instant} in the execution model.
     *
     * @return the scalar
     */
    @Bean
    public GraphQLScalarType instantScalar() {
        Coercing<java.time.Instant, String> coercion = new InstantCoercion();
        // graphql-java 21: Coercing is a functional-ish interface? It declares
        // serialize + parseValue + parseLiteral, so we keep the concrete class.
        return GraphQLScalarType.newScalar()
                .name("Instant")
                .description("An instant in time, ISO-8601 on the wire.")
                .coercing(coercion)
                .build();
    }
}
