package com.infomaximum.server.components.component1.graphql.out.query;

import com.infomaximum.cluster.graphql.anotation.GraphQLField;
import com.infomaximum.cluster.graphql.anotation.GraphQLName;
import com.infomaximum.cluster.graphql.anotation.GraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.struct.GOptional;

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
        } else{
            return 1;
        }
    }
}
