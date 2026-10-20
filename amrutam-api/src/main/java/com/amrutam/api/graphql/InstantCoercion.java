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
 * <p>The input type is {@link Instant} (resolver args are parsed to
 * Instants) and the output type is {@link String} (serialised as ISO-8601),
 * matching the {@code scalar Instant} schema declaration.</p>
 */
public class InstantCoercion implements Coercing<Instant, String> {

    /**
     * Serializes an {@link Instant} to its ISO-8601 string form for the wire.
     *
     * @param input the value
     * @return the ISO-8601 form
     * @throws CoercingSerializeException when the value is null
     */
    @Override
    public String serialize(Object input) throws CoercingSerializeException {
        if (input == null) {
            return null;
        }
        if (input instanceof Instant i) {
            return i.toString();
        }
        if (input instanceof String s) {
            // Tolerate a pre-serialized string; validate it is parseable.
            Instant.parse(s);
            return s;
        }
        throw new CoercingSerializeException("Cannot serialize Instant from " + input);
    }

    /**
     * Parses a variable / literal value back into an {@link Instant}.
     *
     * @param input the incoming value (String or Instant)
     * @return the parsed instant
     * @throws CoercingParseValueException when the value is not parseable
     */
    @Override
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
    @Override
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
