package com.infomaximum.server.components.frontend;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.core.service.transport.executor.ExecutorTransport;
import com.infomaximum.cluster.core.service.transport.executor.ExecutorTransportImpl;
import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.graphql.GraphQLExecutor;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.remote.graphql.RControllerGraphQLImpl;
import com.infomaximum.cluster.struct.Component;
import com.infomaximum.cluster.struct.Info;

/**
 * Created by v.bukharkin on 19.05.2017.
 */
public class FrontendComponent extends Component {

    public static final Info INFO = new Info.Builder<>("com.infomaximum.server.components.frontend")
            .withComponentClass(FrontendComponent.class)
            .build();

    private GraphQLExecutor graphQLExecutor;

    public FrontendComponent(Cluster cluster) {
        super(cluster);
    }

    @Override
    public void load() throws ClusterException {
        try {
            graphQLExecutor = new GraphQLExecutor.Builder(this)
                    .build();
        } catch (GraphQLExecutorException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ExecutorTransport initExecutorTransport() throws ClusterException {
        try {
            return new ExecutorTransportImpl.Builder(this)
                    .withRemoteController(new com.infomaximum.subsystems.graphql.RControllerGraphQLImpl(this))//Обработчик GraphQL запросов
                    .build();
        } catch (ReflectiveOperationException e) {
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
