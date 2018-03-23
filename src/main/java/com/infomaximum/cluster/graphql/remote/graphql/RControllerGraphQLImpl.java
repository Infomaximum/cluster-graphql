package com.infomaximum.cluster.graphql.remote.graphql;

import com.infomaximum.cluster.core.remote.AbstractRController;
import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.preparecustomfield.PrepareCustomField;
import com.infomaximum.cluster.graphql.schema.GraphQLComponentExecutor;
import com.infomaximum.cluster.graphql.schema.GraphQLSchemaType;
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

    public RControllerGraphQLImpl(T component, Set<PrepareCustomField> prepareCustomFields, TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder, GraphQLSchemaType fieldArgumentConverter) throws GraphQLExecutorException {
        super(component);
        graphQLItemExecutor = new GraphQLComponentExecutor(component, prepareCustomFields, fieldConfigurationBuilder, fieldArgumentConverter);
    }

    @Override
    public ArrayList<RGraphQLType> getGraphQLTypes() {
        return graphQLItemExecutor.getGraphQLTypes();
    }

    @Override
    public Serializable prepare(GRequest request, String keyFieldRequest, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Serializable> arguments) throws GraphQLExecutorDataFetcherException {
        return graphQLItemExecutor.prepare(request, keyFieldRequest, graphQLTypeName, graphQLTypeFieldName, arguments);
    }

    @Override
    //TODO Когда для построения иерархии перейдем на классы необходимо Object заменить на Serializable
    public Object executePrepare(GRequest request, String keyFieldRequest, RemoteObject source) throws GraphQLExecutorDataFetcherException {
        return graphQLItemExecutor.executePrepare(request, keyFieldRequest, source);
    }

    @Override
    //TODO Когда для построения иерархии перейдем на классы необходимо Object заменить на Serializable
    public Object execute(GRequest request, RemoteObject source, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Serializable> arguments) throws GraphQLExecutorDataFetcherException {
        return graphQLItemExecutor.execute(request, source, graphQLTypeName, graphQLTypeFieldName, arguments);
    }

}
