package com.infomaximum.cluster.graphql.executor;

import com.infomaximum.cluster.graphql.executor.struct.GExecutionResult;
import com.infomaximum.cluster.graphql.struct.ContextRequest;
import graphql.ExecutionInput;

public interface GraphQLExecutor {

    GExecutionResult execute(ExecutionInput executionInput);

    void requestCompleted(ContextRequest context);

}
