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

	public HashMap<Long, Boolean> prepareExecute(String requestItemKey, GRequest request, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Serializable> arguments);

	@DisableValidationRemoteMethod
    public Object execute(GRequest gRequest, Object source, String graphQLTypeName, String graphQLTypeFieldName, HashMap<String, Object> arguments) throws GraphQLExecutorDataFetcherException;
}
