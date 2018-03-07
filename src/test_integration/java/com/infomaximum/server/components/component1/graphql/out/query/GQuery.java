package com.infomaximum.server.components.component1.graphql.out.query;

import com.infomaximum.cluster.graphql.anotation.GraphQLField;
import com.infomaximum.cluster.graphql.anotation.GraphQLName;
import com.infomaximum.cluster.graphql.anotation.GraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.struct.GOptional;
import com.infomaximum.subsystems.exception.SubsystemException;
import com.infomaximum.subsystems.querypool.Query;
import com.infomaximum.subsystems.querypool.QueryPool;
import com.infomaximum.subsystems.querypool.QueryTransaction;
import com.infomaximum.subsystems.querypool.ResourceProvider;

/**
 * Created by kris on 30.12.16.
 */

@GraphQLTypeOutObject("query")
public class GQuery {

    @GraphQLField
    public int getValue() {
        return 0;
    }

    @GraphQLField
    public GQueryLevel2 getLevel2() {
        return new GQueryLevel2();
    }

    @GraphQLField
    public Integer getValue(@GraphQLName("k") final GOptional<Integer> k) {
        if (k.isPresent()) {
            return 1 + k.get();
        } else{
            return 1;
        }
    }

//    @GraphQLField
//    public Query<Integer> getQueryValue(@GraphQLName("k") final GOptional<Integer> k) {
//
//        return new Query<Integer>() {
//
//            @Override
//            public void prepare(ResourceProvider resources) {
//                resources.borrowResource(GQuery.class, QueryPool.LockType.SHARED);
//            }
//
//            @Override
//            public Integer execute(QueryTransaction transaction) throws SubsystemException {
//                if (k.isPresent()) {
//                    return 1 + k.get();
//                } else{
//                    return 1;
//                }
//            }
//        };
//    }
}
