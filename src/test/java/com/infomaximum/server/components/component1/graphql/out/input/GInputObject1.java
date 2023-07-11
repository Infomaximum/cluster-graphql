package com.infomaximum.server.components.component1.graphql.out.input;

import com.infomaximum.cluster.graphql.anotation.GraphQLTypeInput;

@GraphQLTypeInput("input_object_1")
public class GInputObject1 {

    @GraphQLTypeInput("alias")
    private String alias;

    public GInputObject1() {

    }

    public String getAlias() {
        return alias;
    }
}
