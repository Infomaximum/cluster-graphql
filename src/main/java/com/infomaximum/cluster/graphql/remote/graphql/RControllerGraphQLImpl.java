package com.infomaximum.cluster.graphql.remote.graphql;

import com.infomaximum.cluster.core.remote.AbstractRController;
import com.infomaximum.cluster.graphql.customfieldargument.CustomFieldArgument;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.preparecustomfield.PrepareCustomField;
import com.infomaximum.cluster.graphql.schema.GraphQLComponentExecutor;
import com.infomaximum.cluster.graphql.schema.build.graphqltype.TypeGraphQLFieldConfigurationBuilder;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.cluster.struct.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


/**
 * Created by kris on 19.11.16.
 */
public class RControllerGraphQLImpl<T extends Component> extends AbstractRController<T> implements RControllerGraphQL {

    private final static Logger log = LoggerFactory.getLogger(RControllerGraphQLImpl.class);

    private final GraphQLComponentExecutor graphQLItemExecutor;

    public RControllerGraphQLImpl(T component, Set<CustomFieldArgument> customEnvTypes, Set<PrepareCustomField> prepareCustomFields, TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder) throws GraphQLExecutorException {
        super(component);
        graphQLItemExecutor = new GraphQLComponentExecutor(component, customEnvTypes, prepareCustomFields, fieldConfigurationBuilder);
    }

    @Override
    public ArrayList<RGraphQLType> getGraphQLTypes() {
        return graphQLItemExecutor.getGraphQLTypes();
    }

    @Override
    public Serializable prepareExecute(GRequest request, String keyFieldRequest, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Serializable> arguments) throws GraphQLExecutorDataFetcherException {
        return (Serializable) graphQLItemExecutor.execute(request, keyFieldRequest, null, graphQLTypeName, graphQLTypeFieldName, arguments, true);
    }

    @Override
    public Object execute(GRequest request, String requestItemKey, Object source, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Serializable> arguments) throws GraphQLExecutorDataFetcherException {
        return graphQLItemExecutor.execute(request, requestItemKey, source, graphQLTypeName, graphQLTypeFieldName, arguments, false);
    }

}
