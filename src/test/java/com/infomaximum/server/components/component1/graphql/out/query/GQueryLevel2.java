package com.infomaximum.server.components.component1.graphql.out.query;

import com.infomaximum.cluster.graphql.anotation.GraphQLField;
import com.infomaximum.cluster.graphql.anotation.GraphQLName;
import com.infomaximum.cluster.graphql.anotation.GraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.struct.GOptional;
import org.checkerframework.checker.nullness.qual.NonNull;

@GraphQLTypeOutObject("query_level2")
public class GQueryLevel2 {

    @GraphQLField
    public static Integer getValue(@GraphQLName("k") final GOptional<Integer> k) {
        if (k.isPresent()) {
            return k.get() + 1;
        } else {
            return 1;
        }
    }

    @GraphQLField("value_not_null")
    public static Integer getValueNonNull(
            @NonNull @GraphQLName("k1") final GOptional<Integer> k1,
            @GraphQLName("k2") final GOptional<Integer> k2,
            @NonNull @GraphQLName("k3") final GOptional<Integer> k3
    ) {
        int result = k1.get() + k3.get();
        if (k2.isPresent() && k2.get() != null) {
            result += k2.get();
        }
        return result;
    }

//    @GraphQLField
//    public static GraphQLQuery<RemoteObject, Integer> getQueryValue(@GraphQLName("k") final GOptional<Integer> k) {
//        return new GraphQLQuery<RemoteObject, Integer>() {
//
//            @Override
//            public void prepare(ResourceProvider resources) {
//
//            }
//
//            @Override
//            public Integer execute(
//                    RemoteObject source,
//                    QueryTransaction transaction
//            ) {
//                if (k.isPresent()) {
//                    return 1 + k.get();
//                } else{
//                    return 1;
//                }
//            }
//        };
//    }
}
