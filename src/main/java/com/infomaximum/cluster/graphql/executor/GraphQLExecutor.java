package com.infomaximum.cluster.graphql.executor;

import graphql.ExecutionInput;
import graphql.ExecutionResult;

public interface GraphQLExecutor {

    ExecutionResult execute(ExecutionInput executionInput);

}
