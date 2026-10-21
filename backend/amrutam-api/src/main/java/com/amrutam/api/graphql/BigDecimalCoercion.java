package com.amrutam.api.graphql;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * GraphQL scalar coercion for the {@code BigDecimal} custom scalar.
 *
 * <p>On the wire we accept numeric values or their string form; in the Java
 * execution model we keep the precise {@link BigDecimal} value.</p>
 */
public class BigDecimalCoercion implements Coercing<BigDecimal, BigDecimal> {

    @Override
    public BigDecimal serialize(Object input) throws CoercingSerializeException {
        if (input == null) {
            return null;
        }
        if (input instanceof BigDecimal bd) {
            return bd;
        }
        if (input instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        if (input instanceof String s) {
            return new BigDecimal(s);
        }
        throw new CoercingSerializeException("Cannot serialize BigDecimal from " + input);
    }

    @Override
    public BigDecimal parseValue(Object input) throws CoercingParseValueException {
        try {
            return asBigDecimal(input);
        } catch (RuntimeException e) {
            throw new CoercingParseValueException(e.getMessage(), e);
        }
    }

    @Override
    public BigDecimal parseLiteral(Object input, Map<String, Object> environment)
            throws CoercingParseLiteralException {
        try {
            return asBigDecimal(input);
        } catch (RuntimeException e) {
            throw new CoercingParseLiteralException(e.getMessage(), e);
        }
    }

    private static BigDecimal asBigDecimal(Object input) {
        if (input instanceof BigDecimal bd) {
            return bd;
        }
        if (input instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        if (input instanceof String s) {
            return new BigDecimal(s);
        }
        throw new IllegalArgumentException("Cannot parse BigDecimal from " + input);
    }
}
