package com.infomaximum.server;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.ComponentBuilder;
import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.graphql.GraphQLEngine;
import com.infomaximum.server.components.component1.Component1;
import com.infomaximum.server.components.frontend.FrontendComponent;
import com.infomaximum.server.sdk.GraphQLQueryCustomField;

public class Server implements AutoCloseable  {

    private final Cluster cluster;
    private final GraphQLEngine graphQLEngine;

    public Server() throws ClusterException {
        INSTANCE = this;

        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
            }
        };

        graphQLEngine = new GraphQLEngine.Builder()
//                .withQueryPoolExecutor(new QueryPoolExecutor() {
//                    @Override
//                    public Object execute(Component component, GRequest request, RemoteObject source, com.infomaximum.cluster.querypool.GraphQLQuery query) {
//                        GraphQLQuery graphQLQuery = (GraphQLQuery) query;
//
//                        try {
//                            return new QueryPool().execute(new Query<Object>() {
//
//                                @Override
//                                public void prepare(ResourceProvider resources) {
//
//                                }
//
//                                @Override
//                                public Object execute(QueryTransaction transaction) throws SubsystemException {
//                                    return graphQLQuery.execute(
//                                            null, null, null, transaction
//                                    );
//                                }
//                            }).get();
//                        } catch (Exception e) {
//                            throw new RuntimeException(e);
//                        }
//                    }
//                })
                .withPrepareCustomField(new GraphQLQueryCustomField())
                .build();

        cluster = new Cluster.Builder(uncaughtExceptionHandler)
                .withComponent(
                        new ComponentBuilder(Component1.class)
                )
                .withComponent(
                        new ComponentBuilder(FrontendComponent.class)
                )
                .build();

    }

    public Cluster getCluster() {
        return cluster;
    }

    public GraphQLEngine getGraphQLEngine() {
        return graphQLEngine;
    }

    @Override
    public void close() {
        cluster.close();

        INSTANCE = null;
    }

    public static Server INSTANCE;
}
