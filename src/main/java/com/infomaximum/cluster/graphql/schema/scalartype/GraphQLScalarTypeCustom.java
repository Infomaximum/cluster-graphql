package com.infomaximum.cluster.graphql.schema.scalartype;

import com.google.common.collect.ImmutableSet;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorInvalidSyntaxException;
import graphql.AssertException;
import graphql.Scalars;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.schema.Coercing;

import java.time.Instant;

/**
 * Created by kris on 19.01.17.
 */
public class GraphQLScalarTypeCustom {

    public static final GraphQLTypeScalar GraphQLBoolean = new GraphQLTypeScalar(
            Scalars.GraphQLBoolean,
            ImmutableSet.of(Boolean.class, boolean.class)
    );

    public static final GraphQLTypeScalar GraphQLString = new GraphQLTypeScalar(
            Scalars.GraphQLString,
            String.class
    );

    public static final GraphQLTypeScalar GraphQLInt = new GraphQLTypeScalar(
            Scalars.GraphQLInt,
            ImmutableSet.of(Integer.class, int.class)
    );

    public static final GraphQLTypeScalar GraphQLLong = new GraphQLTypeScalar(
            Scalars.GraphQLLong,
            ImmutableSet.of(Long.class, long.class)
    );

    public static final GraphQLTypeScalar GraphQLBigDecimal = new GraphQLTypeScalar(
            "bigdecimal",
            Scalars.GraphQLBigDecimal,
            ImmutableSet.of(Double.class, double.class)
    );

    public static final GraphQLTypeScalar GraphQLFloat = new GraphQLTypeScalar(
            "Float", "Built-in Float",
            ImmutableSet.of(Float.class, float.class),
            new Coercing<Float, Float>() {

                @Override
                public Float serialize(Object input) {
                    if (isConvertToNumber(input)) {
                        return toNumber(input).floatValue();
                    } else {
                        throw new RuntimeException("Not support type argument: " + input);
                    }
                }

                @Override
                public Float parseValue(Object input) {
                    if (isConvertToNumber(input)) {
                        return toNumber(input).floatValue();
                    } else {
                        throw new RuntimeException("Not support type argument: " + input);
                    }
                }

                @Override
                public Float parseLiteral(Object input) {
                    if (input instanceof IntValue) {
                        return ((IntValue) input).getValue().floatValue();
                    } else if (input instanceof FloatValue) {
                        return ((FloatValue) input).getValue().floatValue();
                    } else {
                        throw new GraphQLExecutorInvalidSyntaxException("Not support type argument: " + input);
                    }
                }
            }
    );

    public static final GraphQLTypeScalar GraphQLInstant = new GraphQLTypeScalar(
            "Instant", "Built-in Instant",
            Instant.class,
            new Coercing() {

                @Override
                public Object serialize(Object input) {
                    if (input == null) return null;
                    if (input instanceof Instant) {
                        return ((Instant) input).toEpochMilli();
                    } else if (input instanceof Number) {
                        return ((Number) input).longValue();
                    } else {
                        throw new RuntimeException("Not support type argument: " + input);
                    }
                }

                @Override
                public Object parseValue(Object input) {
                    if (isConvertToNumber(input)) {
                        return Instant.ofEpochMilli(toNumber(input).longValue());
                    } else {
                        throw new RuntimeException("Not support type argument: " + input);
                    }
                }

                @Override
                public Object parseLiteral(Object input) {
                    if (input instanceof IntValue) {
                        return Instant.ofEpochMilli(((IntValue) input).getValue().longValue());
                    } else {
                        throw new GraphQLExecutorInvalidSyntaxException("Not support type argument: " + input);
                    }
                }
            }
    );


    public static boolean isConvertToNumber(Object input) {
        return input instanceof Number || input instanceof String;
    }

    public static Number toNumber(Object input) {
        if (input instanceof Number) {
            return (Number) input;
        }
        if (input instanceof String) {
            try {
                return Double.parseDouble((String) input);
            } catch (NumberFormatException e) {
                throw new GraphQLExecutorInvalidSyntaxException(e);
            }
        }
        throw new AssertException("Unexpected case - this call should be protected by a previous call to isNumberIsh()");
    }
}