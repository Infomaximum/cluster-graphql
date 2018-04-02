package com.infomaximum;

import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.server.Server;
import com.infomaximum.server.components.frontend.FrontendComponent;
import com.infomaximum.server.sdk.GRequestContext;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
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

    public static ExecutionResult grapqhlExecutor(String query) {
        FrontendComponent frontendComponent = getServer().getCluster().getAnyComponent(FrontendComponent.class);

        GRequest<GRequestContext> gRequest = new GRequest<>(
                frontendComponent.getKey(),
                "127.0.0.1",
                new GRequestContext(),
                new HashMap<>(),
                null
        );

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .context(gRequest)
                .root(gRequest) // This we are doing do be backwards compatible
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
