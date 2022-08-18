package com.infomaximum;

import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.graphql.executor.struct.GExecutionResult;
import com.infomaximum.cluster.graphql.struct.ContextRequest;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.server.Server;
import com.infomaximum.server.components.frontend.FrontendComponent;
import graphql.ExecutionInput;
import org.junit.AfterClass;
import org.junit.BeforeClass;

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

    @BeforeClass
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
                "{}", new HashMap<>()
        );

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .context(new ContextRequest() {
                    @Override
                    public GRequest getRequest() {
                        return gRequest;
                    }
                })
                .variables(Collections.emptyMap())
                .build();

        return frontendComponent.getGraphQLExecutor().execute(executionInput);
    }

    @AfterClass
    public static void destroy() throws IOException {
        if (server != null) {
            server.close();
        }
    }
}
