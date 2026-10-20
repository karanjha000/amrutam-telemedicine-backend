package com.amrutam.api.graphql;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

import java.time.Instant;
import java.util.Map;

/**
 * GraphQL scalar coercion for the {@code Instant} custom scalar: ISO-8601
 * strings on the wire, {@link Instant} in the execution model.
 *
 * <p>Used via {@code GraphQLScalarType.newScalar()} in
 * {@link InstantScalarConfig} (both graphql-java classes are transitively on
 * the classpath through {@code spring-boot-starter-graphql}).</p>
 */
public class InstantCoercion implements Coercing<Instant, Instant> {

    /**
     * Serializes an {@link Instant} for the wire.
     *
     * @param input the value
     * @return the serialized form
     * @throws CoercingSerializeException when the value is not representable
     */
    public Instant serialize(Object input) throws CoercingSerializeException {
        return asInstant(input, "serialize");
    }

    /**
     * Parses a variable / literal value back into an {@link Instant}.
     *
     * @param input the incoming value (String or Instant)
     * @return the parsed instant
     * @throws CoercingParseValueException when the value is not parseable
     */
    public Instant parseValue(Object input) throws CoercingParseValueException {
        try {
            return asInstant(input, "parseValue");
        } catch (RuntimeException e) {
            throw new CoercingParseValueException(e.getMessage(), e);
        }
    }

    /**
     * Parses a literal (AST) value back into an {@link Instant}.
     *
     * @param input the literal (String)
     * @param environment the GraphQL environment
     * @return the parsed instant
     * @throws CoercingParseLiteralException when the value is not parseable
     */
    public Instant parseLiteral(Object input, Map<String, Object> environment)
            throws CoercingParseLiteralException {
        try {
            return asInstant(input, "parseLiteral");
        } catch (RuntimeException e) {
            throw new CoercingParseLiteralException(e.getMessage(), e);
        }
    }

    private static Instant asInstant(Object input, String context) {
        if (input instanceof Instant i) {
            return i;
        }
        if (input instanceof String s) {
            return Instant.parse(s);
        }
        throw new IllegalArgumentException("Cannot " + context + " Instant from " + input);
    }
}
