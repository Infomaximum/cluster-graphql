package com.infomaximum.subsystems.graphql;

import com.infomaximum.subsystems.querypool.ResourceProvider;

public abstract class GraphQLQuery<T> implements com.infomaximum.cluster.querypool.GraphQLQuery<T> {

    public abstract void prepare(ResourceProvider resources);

}
