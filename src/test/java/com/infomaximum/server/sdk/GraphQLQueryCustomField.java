package com.infomaximum.server.sdk;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.preparecustomfield.PrepareCustomField;
import com.infomaximum.cluster.graphql.struct.ContextRequest;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.cluster.struct.Component;
import com.infomaximum.subsystems.graphql.GraphQLQuery;
import com.infomaximum.subsystems.querypool.QueryPool;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GraphQLQueryCustomField implements PrepareCustomField<GraphQLQuery> {

    private final QueryPool queryPool;
    private final ConcurrentMap<GRequest, ConcurrentMap<String, GraphQLQuery>> graphQLRequests;

    public GraphQLQueryCustomField() {
        queryPool = new QueryPool();
        graphQLRequests = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isSupport(Class clazz) {
        return (GraphQLQuery.class.isAssignableFrom(clazz));
    }

    @Override
    public Type getEndType(Type genericType) {
        return ((ParameterizedType) genericType).getActualTypeArguments()[1];
    }

    @Override
    public Serializable requestPrepare(Component component, String keyField, GraphQLQuery value, ContextRequest context) {
        ConcurrentMap<String, GraphQLQuery> requestQueries = graphQLRequests.computeIfAbsent(
                context.getRequest(),
                s -> new ConcurrentHashMap<>()
        );

        if (requestQueries.putIfAbsent(keyField, value) != null) {
            throw new RuntimeException("Ошибка в логике работы - дублирующие идентификаторы: " + keyField);
        }

        return new HashMap<>();
    }

    @Override
    public Serializable execute(String keyField, RemoteObject source, ContextRequest context) throws GraphQLExecutorDataFetcherException {
        throw new RuntimeException("TODO разобраться почему не вызывается requestPrepare");

//        GraphQLQuery graphQLQuery = (GraphQLQuery) graphQLRequests.remove(keyField);
//
//        try {
//            return queryPool.execute(new Query<Serializable>() {
//                @Override
//                public void prepare(ResourceProvider resources) {
//                    graphQLQuery.prepare(resources);
//                }
//
//                @Override
//                public Serializable execute(QueryTransaction transaction) throws SubsystemException {
//                    return graphQLQuery.execute(source, transaction);
//                }
//            }).get();
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public void requestCompleted(ContextRequest context) {
    }
}
