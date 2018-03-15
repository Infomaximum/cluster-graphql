package com.infomaximum.cluster.graphql.remote.graphql;

import com.infomaximum.cluster.core.remote.AbstractRController;
import com.infomaximum.cluster.graphql.customfieldargument.CustomFieldArgument;
import com.infomaximum.cluster.graphql.customfield.CustomField;
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


    public RControllerGraphQLImpl(T component, Set<CustomFieldArgument> customEnvTypes, Set<CustomField> customFields, TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder) throws ReflectiveOperationException{
        super(component);
        graphQLItemExecutor = new GraphQLComponentExecutor(component, customEnvTypes, customFields, fieldConfigurationBuilder);
    }

    @Override
    public ArrayList<RGraphQLType> getGraphQLTypes() {
        return graphQLItemExecutor.getGraphQLTypes();
    }

    @Override
    public Object execute(GRequest request, GRequestItem gRequestItem, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Object> arguments) throws GraphQLExecutorDataFetcherException {
        return graphQLItemExecutor.execute(request, gRequestItem, graphQLTypeName, graphQLTypeFieldName, arguments);
    }

}
