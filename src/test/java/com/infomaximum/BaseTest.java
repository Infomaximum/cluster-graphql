package com.infomaximum;

import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.graphql.executor.struct.GExecutionResult;
import com.infomaximum.cluster.graphql.struct.ContextRequest;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.server.Server;
import com.infomaximum.server.components.frontend.FrontendComponent;
import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionInput;
import graphql.GraphQLError;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;

/**
 * Created by kris on 22.04.17.
 * integrationtest_subsystems@leeching.net
 */
public abstract class BaseTest {

    private static Server server;

    @BeforeAll
    public static void init() throws ClusterException {
        server = new Server();
    }

    public static Server getServer() {
        return server;
    }

    public static GExecutionResult grapqhlExecutor(String query) {
        FrontendComponent frontendComponent = getServer().getCluster().getAnyLocalComponent(FrontendComponent.class);

        GRequest gRequest = new GRequest(
                Instant.now(),
                new GRequest.RemoteAddress("127.0.0.1"),
                "{}", new HashMap<>(), null,
                "123e4567-e89b-12d3-a456-426655440000"
        );

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .context(new TestContextRequest(gRequest))
                .variables(Collections.emptyMap())
                .build();

        return frontendComponent.getGraphQLExecutor().execute(executionInput);


    }

    public static class TestContextRequest implements ContextRequest {

        private final GRequest gRequest;

        public TestContextRequest(GRequest gRequest) {
            this.gRequest = gRequest;
        }

        @Override
        public GRequest getRequest() {
            return gRequest;
        }
    }

    @AfterAll
    public static void destroy() throws IOException {
        if (server != null) {
            server.close();
        }
    }

    public static void assertGraphQLError(GExecutionResult executionResult, Class<? extends GraphQLError> classException) {
        if (executionResult.getErrors().size() != 1
                || executionResult.getErrors().get(0).getClass() != classException) {
            AssertionFailureBuilder.assertionFailure().buildAndThrow();
        }
    }

    public static void assertGraphQLDataFetchingError(GExecutionResult executionResult, Class<? extends Exception> classException) {
        if (executionResult.getErrors().size() != 1
                || executionResult.getErrors().get(0).getClass() != ExceptionWhileDataFetching.class) {
            AssertionFailureBuilder.assertionFailure().buildAndThrow();
        }
        ExceptionWhileDataFetching dataFetching = (ExceptionWhileDataFetching) executionResult.getErrors().get(0);
        if (dataFetching.getException().getClass() != classException) {
            AssertionFailureBuilder.assertionFailure().buildAndThrow();
        }
    }
}
