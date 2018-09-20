package com.infomaximum.cluster.graphql.remote.graphql;

import com.infomaximum.cluster.core.remote.struct.RController;
import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.struct.ContextRequest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by kris on 02.11.16.
 */
public interface RControllerGraphQL extends RController {

	public ArrayList<RGraphQLType> getGraphQLTypes();

    public Serializable prepare(String requestItemKey, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Serializable> arguments, ContextRequest context) throws GraphQLExecutorDataFetcherException;

    public Serializable executePrepare(String keyFieldRequest, RemoteObject source, ContextRequest context) throws GraphQLExecutorDataFetcherException;

    public Serializable execute(RemoteObject source, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Serializable> arguments, ContextRequest context) throws GraphQLExecutorDataFetcherException;
}
