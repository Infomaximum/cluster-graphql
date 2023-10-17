package com.infomaximum.server.components.component1.graphql.out.input;

import com.infomaximum.cluster.graphql.anotation.GraphQLName;
import com.infomaximum.cluster.graphql.anotation.GraphQLTypeInput;

@GraphQLTypeInput("input_object_1")
public class GInputObject1 {

    private String alias;

    public GInputObject1(
            @GraphQLName("alias") String alias
    ) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }
}
