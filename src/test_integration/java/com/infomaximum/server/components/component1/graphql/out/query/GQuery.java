package com.infomaximum.server.components.component1.graphql.out.query;

import com.infomaximum.cluster.graphql.anotation.GraphQLField;
import com.infomaximum.cluster.graphql.anotation.GraphQLTypeOutObject;

/**
 * Created by kris on 30.12.16.
 */

@GraphQLTypeOutObject("query")
public class GQuery {

    @GraphQLField
    public int getValue() {
        return 0;
    }

}
