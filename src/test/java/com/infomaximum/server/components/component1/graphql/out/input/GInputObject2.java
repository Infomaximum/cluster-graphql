package com.infomaximum.server.components.component1.graphql.out.input;

import com.infomaximum.cluster.graphql.anotation.GraphQLName;
import com.infomaximum.cluster.graphql.anotation.GraphQLTypeInput;
import org.checkerframework.checker.nullness.qual.NonNull;

@GraphQLTypeInput("input_object_2")
public class GInputObject2 {

    private final String alias;

    public GInputObject2(
            @NonNull @GraphQLName("alias") String alias,
            @GraphQLName("fake") String fake
    ) {
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }
}
