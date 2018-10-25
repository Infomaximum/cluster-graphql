package com.infomaximum.cluster.graphql.remote.graphql.subscribe;

import com.infomaximum.cluster.core.remote.AbstractRController;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.executor.subscription.GraphQLSubscribeEngineImpl;
import com.infomaximum.cluster.struct.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;


/**
 * Created by kris on 19.11.16.
 */
public class RControllerGraphQLSubscribeImpl<T extends Component> extends AbstractRController<T> implements RControllerGraphQLSubscribe {

    private final static Logger log = LoggerFactory.getLogger(RControllerGraphQLSubscribeImpl.class);

    private final GraphQLSubscribeEngineImpl subscribeEngine;

    public RControllerGraphQLSubscribeImpl(T component, GraphQLSubscribeEngineImpl subscribeEngine) throws GraphQLExecutorException {
        super(component);
        this.subscribeEngine = subscribeEngine;
    }

    @Override
    public void pushEvent(String subscribeKey, Serializable value) {
        subscribeEngine.pushEvent(subscribeKey, value);
    }
}
