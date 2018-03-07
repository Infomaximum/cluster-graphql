package com.infomaximum.subsystems.graphql;

import com.infomaximum.cluster.graphql.remote.graphql.RControllerGraphQL;
import com.infomaximum.cluster.querypool.QueryPoolExecutor;
import com.infomaximum.cluster.struct.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public class RControllerGraphQLImpl<T extends Component> extends com.infomaximum.cluster.graphql.remote.graphql.RControllerGraphQLImpl<T> implements RControllerGraphQL {

    private final static Logger log = LoggerFactory.getLogger(RControllerGraphQLImpl.class);

    public RControllerGraphQLImpl(T component) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        super(component, new QueryPoolExecutor(){

        }, null, null);
    }

}
