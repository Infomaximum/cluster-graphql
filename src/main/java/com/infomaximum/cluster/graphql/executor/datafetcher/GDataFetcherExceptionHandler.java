package com.infomaximum.cluster.graphql.executor.datafetcher;

import graphql.ExceptionWhileDataFetching;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import graphql.language.SourceLocation;

import java.util.concurrent.CompletableFuture;

public class GDataFetcherExceptionHandler implements DataFetcherExceptionHandler {

    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(DataFetcherExceptionHandlerParameters handlerParameters) {
        Throwable exception = handlerParameters.getException();
        SourceLocation sourceLocation = handlerParameters.getSourceLocation();
        ResultPath path = handlerParameters.getPath();

        ExceptionWhileDataFetching error = new ExceptionWhileDataFetching(path, exception, sourceLocation);
        handlerException(exception);
        return CompletableFuture.completedFuture(DataFetcherExceptionHandlerResult.newResult().error(error).build());
    }

    public void handlerException(Throwable exception) {
    }
}
