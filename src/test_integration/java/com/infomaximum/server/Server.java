package com.infomaximum.server;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.ComponentBuilder;
import com.infomaximum.cluster.builder.transport.MockTransportBuilder;
import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.server.components.component1.Component1;
import com.infomaximum.server.components.frontend.FrontendComponent;

public class Server implements AutoCloseable  {

    private final Cluster cluster;

    public Server() throws ClusterException {
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

    @Override
    public void close() {
        cluster.close();
    }
}
