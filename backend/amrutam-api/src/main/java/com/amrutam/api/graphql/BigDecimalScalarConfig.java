package com.amrutam.api.graphql;

import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.math.BigDecimal;

/**
 * Registers the custom {@code BigDecimal} GraphQL scalar with the runtime.
 *
 * <p>Spring GraphQL requires the scalar to be added to runtime wiring to match
 * the schema declarations used in the API.</p>
 */
@Configuration
public class BigDecimalScalarConfig {

    @Bean
    public GraphQLScalarType bigDecimalScalar() {
        Coercing<BigDecimal, BigDecimal> coercing = new BigDecimalCoercion();
        return GraphQLScalarType.newScalar()
                .name("BigDecimal")
                .description("A precise decimal number value.")
                .coercing(coercing)
                .build();
    }

    @Bean
    public RuntimeWiringConfigurer bigDecimalWiringConfigurer(GraphQLScalarType bigDecimalScalar) {
        return builder -> builder.scalar(bigDecimalScalar);
    }
}
