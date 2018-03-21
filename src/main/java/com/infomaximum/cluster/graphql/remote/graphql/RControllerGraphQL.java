package com.infomaximum.cluster.graphql.remote.graphql;

import com.infomaximum.cluster.anotation.DisableValidationRemoteMethod;
import com.infomaximum.cluster.core.remote.struct.RController;
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

	public Serializable prepareExecute(GRequest request, String requestItemKey, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Serializable> arguments) throws GraphQLExecutorDataFetcherException;

	@DisableValidationRemoteMethod
    public Object execute(GRequest gRequest, String requestItemKey, Object source, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Serializable> arguments) throws GraphQLExecutorDataFetcherException;
}
