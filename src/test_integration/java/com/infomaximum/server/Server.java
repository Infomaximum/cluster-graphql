package com.infomaximum.server;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.ComponentBuilder;
import com.infomaximum.cluster.builder.transport.MockTransportBuilder;
import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.graphql.GraphQLEngine;
import com.infomaximum.server.components.component1.Component1;
import com.infomaximum.server.components.frontend.FrontendComponent;

public class Server implements AutoCloseable  {

    private final Cluster cluster;
    private final GraphQLEngine graphQLEngine;

    public Server() throws ClusterException {
        INSTANCE = this;

        graphQLEngine = new GraphQLEngine.Builder().build();

        cluster = new Cluster.Builder()
                .withTransport(
                        new MockTransportBuilder()
                )
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
