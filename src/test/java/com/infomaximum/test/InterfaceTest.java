package com.infomaximum.test;

import com.infomaximum.BaseTest;
import com.infomaximum.cluster.graphql.executor.GraphQLExecutorPrepareImpl;
import com.infomaximum.cluster.graphql.executor.struct.GExecutionResult;
import com.infomaximum.cluster.graphql.schema.build.MergeGraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;
import com.infomaximum.server.components.frontend.FrontendComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

public class InterfaceTest extends BaseTest {

    @Test
    public void checkContainsMethods() throws NoSuchFieldException, IllegalAccessException {
        Assertions.assertTrue(isContainFieldInSchema("interface1", "static_method"));
        Assertions.assertTrue(isContainFieldInSchema("interface1", "default_method"));
    }

    @Test
    public void testStaticMethod() {
        GExecutionResult executionResult = grapqhlExecutor("{interface1{static_method}}");
        Assertions.assertTrue(executionResult.getErrors().isEmpty());

        Assertions.assertEquals("static_method", ((Map) ((Map) executionResult.getData()).get("interface1")).get("static_method"));
    }

    @Test
    public void testDefaultMethod1() {
        GExecutionResult executionResult = grapqhlExecutor("{interface1{default_method}}");
        Assertions.assertTrue(executionResult.getErrors().isEmpty());

        Assertions.assertEquals("default_method", ((Map) ((Map) executionResult.getData()).get("interface1")).get("default_method"));
    }

    @Test
    public void testDefaultMethod2() {
        GExecutionResult executionResult = grapqhlExecutor("{interface2{default_method}}");
        Assertions.assertTrue(executionResult.getErrors().isEmpty());

        Assertions.assertEquals("default_method2", ((Map) ((Map) executionResult.getData()).get("interface2")).get("default_method"));
    }

    public static boolean isContainFieldInSchema(String type, String field) throws NoSuchFieldException, IllegalAccessException {
        FrontendComponent frontendComponent = getServer().getCluster().getAnyLocalComponent(FrontendComponent.class);

        GraphQLExecutorPrepareImpl graphQLExecutor = (GraphQLExecutorPrepareImpl) frontendComponent.getGraphQLExecutor();

        Field iField = GraphQLExecutorPrepareImpl.class.getDeclaredField("remoteGraphQLTypeOutObjects");
        iField.setAccessible(true);

        Map<String, MergeGraphQLTypeOutObject> remoteGraphQLTypeOutObjects = (Map<String, MergeGraphQLTypeOutObject>) iField.get(graphQLExecutor);
        MergeGraphQLTypeOutObject graphQLTypeOutObject = remoteGraphQLTypeOutObjects.get(type);

        for (RGraphQLObjectTypeField item : graphQLTypeOutObject.getFields()) {
            if (item.externalName.equals(field)) {
                return true;
            }
        }
        return false;
    }
}