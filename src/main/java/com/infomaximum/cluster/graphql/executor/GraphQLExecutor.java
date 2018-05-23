package com.infomaximum.cluster.graphql.executor;

import com.infomaximum.cluster.graphql.struct.ContextRequest;
import graphql.ExecutionInput;
import graphql.ExecutionResult;

public interface GraphQLExecutor {

    ExecutionResult execute(ExecutionInput executionInput);

    void requestCompleted(Throwable ex, ContextRequest context);
}
