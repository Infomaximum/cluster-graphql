package com.infomaximum.cluster.graphql.executor;

import com.infomaximum.cluster.graphql.struct.GRequest;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;

public class GraphQLExecutorImpl implements GraphQLExecutor {

    private final GraphQLSchema schema;
    private final GraphQL graphQL;

    public GraphQLExecutorImpl(GraphQLSchema schema, GraphQL graphQL) {
        this.schema = schema;
        this.graphQL = graphQL;
    }

    public GraphQLSchema getSchema() {
        return schema;
    }

    @Override
    public ExecutionResult execute(ExecutionInput executionInput) {
        return graphQL.execute(executionInput);
    }

    @Override
    public void requestCompleted(GRequest request, Throwable ex) {
    }
}
