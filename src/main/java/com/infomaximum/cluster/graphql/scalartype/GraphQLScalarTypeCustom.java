package com.infomaximum.cluster.graphql.scalartype;

import graphql.AssertException;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;

import java.util.Date;

/**
 * Created by kris on 19.01.17.
 */
public class GraphQLScalarTypeCustom {

    public static GraphQLScalarType GraphQLDate = new GraphQLScalarType("Date", "Built-in Date", new Coercing() {

        @Override
        public Object serialize(Object input) {
            if (input==null) return null;
            if (input instanceof Date) {
                return ((Date) input).getTime();
            } else if (input instanceof Number) {
                return ((Number)input).longValue();
            } else {
                throw new RuntimeException("Not support type argument: " + input);
            }
        }

        @Override
        public Object parseValue(Object input) {
            if (input instanceof Number) {
                return new Date(((Number)input).longValue());
            } else {
                return null;
            }
        }

        @Override
        public Object parseLiteral(Object input) {
            if (input instanceof IntValue) {
                return new Date(((IntValue)input).getValue().longValue());
            } else {
                return null;
            }
        }
    });

    public static GraphQLScalarType GraphQLFloat = new GraphQLScalarType("Float", "Built-in Float", new Coercing<Float, Float>() {
        @Override
        public Float serialize(Object input) {
            if (input instanceof Float) {
                return (Float) input;
            } else if (isNumberIsh(input)) {
                return toNumber(input).floatValue();
            } else {
                return null;
            }
        }

        @Override
        public Float parseValue(Object input) {
            return serialize(input);
        }

        @Override
        public Float parseLiteral(Object input) {
            if (input instanceof IntValue) {
                return ((IntValue) input).getValue().floatValue();
            } else if (input instanceof FloatValue) {
                return ((FloatValue) input).getValue().floatValue();
            } else {
                return null;
            }
        }
    });


    private static boolean isNumberIsh(Object input) {
        return input instanceof Number || input instanceof String;
    }

    private static Number toNumber(Object input) {
        if (input instanceof Number) {
            return (Number) input;
        }
        if (input instanceof String) {
            return Double.parseDouble((String) input);
        }
        throw new AssertException("Unexpected case - this call should be protected by a previous call to isNumberIsh()");
    }
}