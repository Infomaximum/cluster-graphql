package com.infomaximum.cluster.graphql.remote.graphql;

import com.infomaximum.cluster.core.remote.AbstractRController;
import com.infomaximum.cluster.graphql.customtype.CustomEnvType;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.schema.GraphQLComponentExecutor;
import com.infomaximum.cluster.graphql.schema.build.graphqltype.TypeGraphQLFieldConfigurationBuilder;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.cluster.graphql.struct.GRequestItem;
import com.infomaximum.cluster.struct.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;


/**
 * Created by kris on 19.11.16.
 */
public class RControllerGraphQLImpl<T extends Component> extends AbstractRController<T> implements RControllerGraphQL {

    private final static Logger log = LoggerFactory.getLogger(RControllerGraphQLImpl.class);

    private final GraphQLComponentExecutor graphQLItemExecutor;


    public RControllerGraphQLImpl(T component, Set<CustomEnvType> customEnvTypes, TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder) throws ReflectiveOperationException{
        super(component);
        graphQLItemExecutor = new GraphQLComponentExecutor(component, customEnvTypes, fieldConfigurationBuilder);
    }

    @Override
    public List<RGraphQLType> getCustomTypes() {
        return graphQLItemExecutor.getCustomTypes();
    }

    @Override
    public Map<Long, Boolean> prepareRequest(String requestQueryKey, String graphQLTypeName, String graphQLTypeFieldName) {
        return null;
    }

    @Override
    public Object execute(String requestQueryKey, GRequest request, GRequestItem gRequestItem, String graphQLTypeName, String graphQLTypeFieldName, Map<String, Object> arguments) throws GraphQLExecutorDataFetcherException {
        return graphQLItemExecutor.execute(request, gRequestItem, graphQLTypeName, graphQLTypeFieldName, arguments);
    }

}
