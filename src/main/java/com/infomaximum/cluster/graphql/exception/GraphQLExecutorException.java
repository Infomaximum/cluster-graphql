package com.infomaximum.cluster.graphql.exception;

public class GraphQLExecutorException extends Exception {

    public GraphQLExecutorException(String message) {
        super(message);
    }

    public GraphQLExecutorException(String message, Throwable cause) {
        super(message, cause);
    }

    public GraphQLExecutorException(Throwable cause) {
        super(cause);
    }
}
