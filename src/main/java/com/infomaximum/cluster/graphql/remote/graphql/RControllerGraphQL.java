package com.infomaximum.cluster.graphql.remote.graphql;

import com.infomaximum.cluster.anotation.DisableValidationRemoteMethod;
import com.infomaximum.cluster.core.remote.struct.RController;
import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.struct.GRequest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by kris on 02.11.16.
 */
public interface RControllerGraphQL extends RController {

	public ArrayList<RGraphQLType> getGraphQLTypes();

    public Serializable prepare(GRequest request, String requestItemKey, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Serializable> arguments) throws GraphQLExecutorDataFetcherException;

    //TODO Когда для построения иерархии перейдем на классы необходимо Object заменить на Serializable
    @DisableValidationRemoteMethod
    public Object executePrepare(GRequest request, String keyFieldRequest, RemoteObject source) throws GraphQLExecutorDataFetcherException;

    //TODO Когда для построения иерархии перейдем на классы необходимо Object заменить на Serializable
    @DisableValidationRemoteMethod
    public Object execute(GRequest gRequest, RemoteObject source, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Serializable> arguments) throws GraphQLExecutorDataFetcherException;
}
