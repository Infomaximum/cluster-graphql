package com.infomaximum.server.sdk;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.core.service.transport.executor.ExecutorTransportImpl;
import com.infomaximum.cluster.graphql.remote.graphql.executor.RControllerGraphQLExecutorImpl;
import com.infomaximum.server.Server;

public class Component extends com.infomaximum.cluster.struct.Component {

    private RControllerGraphQLExecutorImpl rControllerGraphQLExecutor;

    public Component(Cluster cluster) {
        super(cluster);
    }

    @Override
    protected void registerComponent() {
        super.registerComponent();
        this.rControllerGraphQLExecutor.init();
    }

    @Override
    protected ExecutorTransportImpl.Builder getExecutorTransportBuilder() {
        this.rControllerGraphQLExecutor = Server.INSTANCE.getGraphQLEngine().buildRemoteControllerGraphQLExecutor(this);//Обработчик GraphQL запросов
        return super.getExecutorTransportBuilder()
                .withRemoteController(rControllerGraphQLExecutor);
    }

}
