package com.infomaximum.server.components.component1.graphql.out.query;

import com.infomaximum.cluster.graphql.anotation.GraphQLField;
import com.infomaximum.cluster.graphql.anotation.GraphQLName;
import com.infomaximum.cluster.graphql.anotation.GraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.struct.GOptional;
import com.infomaximum.server.components.component1.graphql.out.input.GInputObject1;
import com.infomaximum.server.components.component1.graphql.out.input.GInputObject2;
import com.infomaximum.server.components.component1.graphql.out.input.GInputObjectWithException;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Created by kris on 30.12.16.
 */

@GraphQLTypeOutObject("query")
public class GQuery {

    @GraphQLField
    public static int getValue() {
        return 0;
    }

    @GraphQLField
    public static Class<GQueryLevel2> getLevel2() {
        return GQueryLevel2.class;
    }

    @GraphQLField
    public static Integer getValue1(@GraphQLName("k") final GOptional<Integer> k) {
        if (k.isPresent()) {
            return 1 + k.get();
        } else {
            return 1;
        }
    }

    @GraphQLField
    public static int getValue2(@NonNull @GraphQLName("k") final int k) {
        return k + 1;
    }

    @GraphQLField
    public static int getValue3(@NonNull @GraphQLName("k") final GInputObject1 input) {
        return (input.getAlias() == null) ? 0 : input.getAlias().length();
    }

    @GraphQLField
    public static int getValue4(@NonNull @GraphQLName("k") final GInputObject2 input) {
        return (input.getAlias() == null) ? 0 : input.getAlias().length();
    }

    @GraphQLField
    public static int getValue5(@NonNull @GraphQLName("k") final GInputObjectWithException input) {
        return (input.getAlias() == null) ? 0 : input.getAlias().length();
    }
}
