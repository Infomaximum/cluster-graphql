package com.infomaximum.cluster.graphql.remote.graphql;

import com.infomaximum.cluster.core.remote.AbstractRController;
import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.customtype.CustomEnvType;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.schema.GraphQLComponentExecutor;
import com.infomaximum.cluster.graphql.schema.build.graphqltype.TypeGraphQLFieldConfigurationBuilder;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.cluster.graphql.struct.GRequestItem;
import com.infomaximum.cluster.querypool.QueryPoolExecutor;
import com.infomaximum.cluster.struct.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.*;


/**
 * Created by kris on 19.11.16.
 */
public class RControllerGraphQLImpl<T extends Component> extends AbstractRController<T> implements RControllerGraphQL {

    private final static Logger log = LoggerFactory.getLogger(RControllerGraphQLImpl.class);

    private final GraphQLComponentExecutor graphQLItemExecutor;

    public RControllerGraphQLImpl(T component, QueryPoolExecutor queryExecutor) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        super(component);
        graphQLItemExecutor = new GraphQLComponentExecutor(component);
    }

    public RControllerGraphQLImpl(T component, QueryPoolExecutor queryExecutor, Set<CustomEnvType> customEnvTypes, TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        super(component);
        graphQLItemExecutor = new GraphQLComponentExecutor(component, customEnvTypes, fieldConfigurationBuilder);
    }

    @Override
    public List<RGraphQLType> getCustomTypes() {
        return graphQLItemExecutor.getCustomTypes();
    }

    @Override
    public Object execute(GRequest request, GRequestItem gRequestItem, String graphQLTypeName, String graphQLTypeMethodName, Map<String, Object> arguments) throws GraphQLExecutorDataFetcherException {
        return graphQLItemExecutor.execute(request, gRequestItem, graphQLTypeName, graphQLTypeMethodName, arguments);
    }

}
