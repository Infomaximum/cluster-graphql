package com.infomaximum.server.components.component1.graphql.out.query;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.anotation.GraphQLField;
import com.infomaximum.cluster.graphql.anotation.GraphQLName;
import com.infomaximum.cluster.graphql.anotation.GraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.struct.GOptional;
import com.infomaximum.subsystems.exception.SubsystemException;
import com.infomaximum.subsystems.graphql.GraphQLQuery;
import com.infomaximum.subsystems.querypool.QueryTransaction;
import com.infomaximum.subsystems.querypool.ResourceProvider;

@GraphQLTypeOutObject("query_level2")
public class GQueryLevel2 {

    @GraphQLField
    public static Integer getValue(@GraphQLName("k") final GOptional<Integer> k) {
        if (k.isPresent()) {
            return 1 + k.get();
        } else{
            return 1;
        }
    }

    @GraphQLField
    public static GraphQLQuery<RemoteObject, Integer> getQueryValue(@GraphQLName("k") final GOptional<Integer> k) {

        return new GraphQLQuery<RemoteObject, Integer>() {

            @Override
            public void prepare(ResourceProvider resources) {

            }

            @Override
            public Integer execute(
                    RemoteObject source,
                    QueryTransaction transaction
            ) throws SubsystemException {

                if (k.isPresent()) {
                    return 1 + k.get();
                } else{
                    return 1;
                }
            }


        };
    }
}
