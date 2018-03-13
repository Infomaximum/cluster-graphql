package com.infomaximum.cluster.graphql.remote.graphql;

import com.infomaximum.cluster.anotation.DisableValidationRemoteMethod;
import com.infomaximum.cluster.core.remote.struct.RController;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.cluster.graphql.struct.GRequestItem;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by kris on 02.11.16.
 */
public interface RControllerGraphQL extends RController {

	public ArrayList<RGraphQLType> getCustomTypes();

	@DisableValidationRemoteMethod
    public Object execute(GRequest gRequest, GRequestItem gRequestItem, String graphQLTypeName, String graphQLTypeMethodName, HashMap<String, Object> arguments) throws GraphQLExecutorDataFetcherException;
}
