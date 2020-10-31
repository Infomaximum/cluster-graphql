package com.infomaximum.cluster.graphql.remote.graphql.executor;

import com.infomaximum.cluster.core.remote.AbstractRController;
import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.executor.component.GraphQLComponentExecutor;
import com.infomaximum.cluster.graphql.schema.GraphQLSchemaType;
import com.infomaximum.cluster.graphql.schema.build.graphqltype.TypeGraphQLFieldConfigurationBuilder;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.struct.ContextRequest;
import com.infomaximum.cluster.struct.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by kris on 19.11.16.
 */
public class RControllerGraphQLExecutorImpl<T extends Component> extends AbstractRController<T> implements RControllerGraphQLExecutor {

    private final static Logger log = LoggerFactory.getLogger(RControllerGraphQLExecutorImpl.class);

    private final TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder;
    private final GraphQLSchemaType fieldArgumentConverter;

    private GraphQLComponentExecutor graphQLItemExecutor;

    public RControllerGraphQLExecutorImpl(T component, TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder, GraphQLSchemaType fieldArgumentConverter) throws GraphQLExecutorException {
        super(component);
        this.fieldConfigurationBuilder = fieldConfigurationBuilder;
        this.fieldArgumentConverter = fieldArgumentConverter;
    }

    public void init() {
        if (graphQLItemExecutor != null) {
            throw new IllegalStateException();
        }
        graphQLItemExecutor = new GraphQLComponentExecutor(component, fieldConfigurationBuilder, fieldArgumentConverter);
    }

    @Override
    public ArrayList<RGraphQLType> getGraphQLTypes() {
        return graphQLItemExecutor.getGraphQLTypes();
    }

    @Override
    public Serializable prepare(String keyFieldRequest, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Serializable> arguments, ContextRequest context) throws GraphQLExecutorDataFetcherException {
        return graphQLItemExecutor.prepare(component, keyFieldRequest, graphQLTypeName, graphQLTypeFieldName, arguments, context);
    }

    @Override
    public Serializable executePrepare(String keyFieldRequest, RemoteObject source, ContextRequest context) throws GraphQLExecutorDataFetcherException {
        return graphQLItemExecutor.executePrepare(keyFieldRequest, source, context);
    }

    @Override
    public Serializable execute(RemoteObject source, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Serializable> arguments, ContextRequest context) throws GraphQLExecutorDataFetcherException {
        return graphQLItemExecutor.execute(source, graphQLTypeName, graphQLTypeFieldName, arguments, context);
    }

}
