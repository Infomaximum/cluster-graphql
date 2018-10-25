package com.infomaximum.server.components.frontend;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.core.service.transport.executor.ExecutorTransport;
import com.infomaximum.cluster.core.service.transport.executor.ExecutorTransportImpl;
import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
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
    }

    @Override
    public void load() throws ClusterException {
        try {
            graphQLExecutor = Server.INSTANCE.getGraphQLEngine().buildExecutor(this, graphQLSubscribeEngine);
        } catch (GraphQLExecutorException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ExecutorTransport initExecutorTransport() throws ClusterException {
        try {
            return new ExecutorTransportImpl.Builder(this)
                    .withRemoteController(
                            Server.INSTANCE.getGraphQLEngine().buildRemoteControllerGraphQLSubscribe(this, graphQLSubscribeEngine)//Обработчик GraphQL опопвещений подписчиков
                    )
                    .withRemoteController(
                            Server.INSTANCE.getGraphQLEngine().buildRemoteControllerGraphQLExecutor(this)//Обработчик GraphQL запросов
                    )
                    .build();
        } catch (GraphQLExecutorException e) {
            throw new ClusterException(e);
        }
    }

    @Override
    public Info getInfo() {
        return INFO;
    }

    public GraphQLExecutor getGraphQLExecutor() {
        return graphQLExecutor;
    }

    @Override
    public void destroying() throws ClusterException {
        // do nothing
    }
}
