package com.infomaximum.server.components.frontend;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.core.service.transport.executor.ExecutorTransportImpl;
import com.infomaximum.cluster.graphql.executor.GraphQLExecutor;
import com.infomaximum.cluster.graphql.executor.subscription.GraphQLSubscribeEngine;
import com.infomaximum.cluster.struct.Component;
import com.infomaximum.cluster.struct.Info;
import com.infomaximum.server.Server;

/**
 * Created by kris.
 */
public class FrontendComponent extends Component {

    public static final Info INFO = new Info.Builder<>("com.infomaximum.server.components.frontend")
            .withComponentClass(FrontendComponent.class)
            .build();

    private final GraphQLSubscribeEngine graphQLSubscribeEngine;
    private GraphQLExecutor graphQLExecutor;

    public FrontendComponent(Cluster cluster) {
        super(cluster);
        this.graphQLSubscribeEngine = Server.INSTANCE.getGraphQLEngine().buildSubscribeEngine();

        this.graphQLExecutor = Server.INSTANCE.getGraphQLEngine().buildExecutor(this, graphQLSubscribeEngine);
    }

    @Override
    protected ExecutorTransportImpl.Builder getExecutorTransportBuilder() {
        return super.getExecutorTransportBuilder()
                .withRemoteController(
                        Server.INSTANCE.getGraphQLEngine().buildRemoteControllerGraphQLSubscribe(this, graphQLSubscribeEngine)//Обработчик GraphQL опопвещений подписчиков
                )
                .withRemoteController(
                        Server.INSTANCE.getGraphQLEngine().buildRemoteControllerGraphQLExecutor(this)//Обработчик GraphQL запросов
                );
    }

    @Override
    public Info getInfo() {
        return INFO;
    }

    public GraphQLExecutor getGraphQLExecutor() {
        return graphQLExecutor;
    }
}
