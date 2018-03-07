package com.infomaximum.subsystems.querypool;

import com.infomaximum.cluster.querypool.GraphQLQuery;
import com.infomaximum.subsystems.exception.SubsystemException;

public abstract class Query<T> implements GraphQLQuery {

    public abstract void prepare(ResourceProvider resources);

    public QueryPool.Priority getPriority(){
        return QueryPool.Priority.HIGH;
    }

    public String getMaintenanceMarker(){
        return null;
    }

    public abstract T execute(QueryTransaction transaction) throws SubsystemException;

    public void onTransactionCommitted() {}

    public void onTransactionRollbacks() {}
}
