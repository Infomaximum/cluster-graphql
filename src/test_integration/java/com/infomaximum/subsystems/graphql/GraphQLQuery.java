package com.infomaximum.subsystems.graphql;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.subsystems.exception.SubsystemException;
import com.infomaximum.subsystems.querypool.QueryTransaction;
import com.infomaximum.subsystems.querypool.ResourceProvider;

import java.io.Serializable;

public abstract class GraphQLQuery<S extends RemoteObject, T extends Serializable> {

    public abstract void prepare(ResourceProvider resources);

    public abstract T execute(
            S source,
            QueryTransaction transaction
    ) throws SubsystemException;

    public void onTransactionCommitted() {
    }

    public void onTransactionRollbacks() {
    }
}
