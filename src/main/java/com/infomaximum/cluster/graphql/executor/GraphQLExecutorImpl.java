package com.infomaximum.cluster.graphql.executor;

import graphql.*;
import graphql.schema.*;

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
}
