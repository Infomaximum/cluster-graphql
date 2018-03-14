package com.infomaximum.cluster.querypool;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.cluster.struct.Component;

public interface QueryPoolExecutor {

    Object execute(Component component, GRequest request, RemoteObject source, GraphQLQuery query);
}
