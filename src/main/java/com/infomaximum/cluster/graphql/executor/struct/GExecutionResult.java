package com.infomaximum.cluster.graphql.executor.struct;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.execution.reactive.CompletionStageMappingPublisher;

import java.util.List;

public class GExecutionResult {

    private final ExecutionResult executionResult;

    public GExecutionResult(ExecutionResult executionResult) {
        this.executionResult = executionResult;
    }

    public <T> T getData() {
        if (executionResult.getData() instanceof CompletionStageMappingPublisher csmp) {
            return (T) new GCompletionStageMappingPublisher(csmp);
        } else {
            return executionResult.getData();
        }
    }

    public List<GraphQLError> getErrors() {
        return executionResult.getErrors();
    }

}
