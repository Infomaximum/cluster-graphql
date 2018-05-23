package com.infomaximum.server.components.component1;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.core.service.transport.executor.ExecutorTransport;
import com.infomaximum.cluster.core.service.transport.executor.ExecutorTransportImpl;
import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.remote.graphql.RControllerGraphQLImpl;
import com.infomaximum.cluster.struct.Component;
import com.infomaximum.cluster.struct.Info;
import com.infomaximum.server.Server;

/**
 * Created by v.bukharkin on 19.05.2017.
 */
public class Component1 extends Component {

    public static final Info INFO = new Info.Builder<>("com.infomaximum.server.components.component1")
            .withComponentClass(Component1.class)
            .build();

    public Component1(Cluster cluster) {
        super(cluster);
    }

    @Override
    public void load() throws ClusterException {

    }

    @Override
    public ExecutorTransport initExecutorTransport() throws ClusterException {
        try {
            return new ExecutorTransportImpl.Builder(this)
                    .withRemoteController(
                            Server.INSTANCE.getGraphQLEngine().buildRemoteControllerGraphQL(this)//Обработчик GraphQL запросов
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


    @Override
    public void destroying() throws ClusterException {
        // do nothing
    }
}
