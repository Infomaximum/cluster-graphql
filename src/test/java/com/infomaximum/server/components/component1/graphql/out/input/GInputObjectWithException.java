package com.infomaximum.server.components.component1.graphql.out.input;

import com.infomaximum.cluster.graphql.anotation.GraphQLName;
import com.infomaximum.cluster.graphql.anotation.GraphQLTypeInput;
import org.checkerframework.checker.nullness.qual.NonNull;

@GraphQLTypeInput("input_object_with_exception")
public class GInputObjectWithException {

    private final String alias;

    public GInputObjectWithException(
            @NonNull @GraphQLName("alias") String alias,
            @GraphQLName("fake") String fake
    ) {
        throw new IllegalArgumentException();
    }

    public String getAlias() {
        return alias;
    }
}
