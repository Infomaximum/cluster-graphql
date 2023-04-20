package com.infomaximum.server.components.frontend;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.core.service.transport.executor.ComponentExecutorTransportImpl;
import com.infomaximum.cluster.graphql.executor.GraphQLExecutor;
import com.infomaximum.cluster.graphql.executor.subscription.GraphQLSubscribeEngine;
import com.infomaximum.server.Server;
import com.infomaximum.server.sdk.Component;

/**
 * Created by kris.
 */
@com.infomaximum.cluster.anotation.Info(uuid = "com.infomaximum.server.components.frontend")
public class FrontendComponent extends Component {

    private final GraphQLSubscribeEngine graphQLSubscribeEngine;
    private GraphQLExecutor graphQLExecutor;

    public FrontendComponent() {
        this.graphQLSubscribeEngine = Server.INSTANCE.getGraphQLEngine().buildSubscribeEngine();
    }

    @Override
    protected ComponentExecutorTransportImpl.Builder getExecutorTransportBuilder() {
        return super.getExecutorTransportBuilder()
                .withRemoteController(
                        Server.INSTANCE.getGraphQLEngine().buildRemoteControllerGraphQLSubscribe(this, graphQLSubscribeEngine)//Обработчик GraphQL опопвещений подписчиков
                );
    }

    @Override
    public void init(Cluster cluster, TransportManager transportManager) {
        super.init(cluster, transportManager);
        this.graphQLExecutor = Server.INSTANCE.getGraphQLEngine().buildExecutor(this, graphQLSubscribeEngine);
    }

    public GraphQLExecutor getGraphQLExecutor() {
        return graphQLExecutor;
    }
}
