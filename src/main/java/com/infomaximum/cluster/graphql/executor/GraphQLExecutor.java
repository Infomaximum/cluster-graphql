package com.infomaximum.cluster.graphql.executor;

import com.infomaximum.cluster.graphql.struct.GRequest;
import graphql.ExecutionInput;
import graphql.ExecutionResult;

public interface GraphQLExecutor {

    ExecutionResult execute(ExecutionInput executionInput);

    void requestCompleted(GRequest request, Throwable ex);
}
