package com.infomaximum.subsystems.graphql;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.cluster.struct.Component;
import com.infomaximum.subsystems.exception.SubsystemException;
import com.infomaximum.subsystems.querypool.QueryTransaction;

public abstract class GraphQLQuery<C extends Component, S extends RemoteObject, T> {

    public abstract T execute(
            C component,
            GRequest request,
            S source,
            QueryTransaction transaction) throws SubsystemException;

}
