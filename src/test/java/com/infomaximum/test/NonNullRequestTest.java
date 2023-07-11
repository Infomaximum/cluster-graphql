package com.infomaximum.test;

import com.infomaximum.BaseTest;
import com.infomaximum.cluster.graphql.executor.struct.GExecutionResult;
import graphql.validation.ValidationError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class NonNullRequestTest extends BaseTest {

    @Test
    public void test1() throws Exception {
        GExecutionResult executionResult1 = grapqhlExecutor("{value2}");
        assertGraphQLError(executionResult1, ValidationError.class);

        GExecutionResult executionResult2 = grapqhlExecutor("{value2(k:1)}");
        Assertions.assertTrue(executionResult2.getErrors().isEmpty());
        Assertions.assertEquals(2, ((Map) executionResult2.getData()).get("value2"));
    }

    @Test
    public void test2() throws Exception {
        GExecutionResult executionResult1 = grapqhlExecutor("{value3}");
        assertGraphQLError(executionResult1, ValidationError.class);

        GExecutionResult executionResult3 = grapqhlExecutor("{value3(k:{alias:\"test\"})}");
        Assertions.assertTrue(executionResult3.getErrors().isEmpty());
        Assertions.assertEquals(4, ((Map) executionResult3.getData()).get("value3"));
    }

    @Test
    public void test3() throws Exception {
        GExecutionResult executionResult1 = grapqhlExecutor("{value4}");
        assertGraphQLError(executionResult1, ValidationError.class);

        GExecutionResult executionResult2 = grapqhlExecutor("{value4(k:{})}");
        assertGraphQLError(executionResult2, ValidationError.class);

        GExecutionResult executionResult3 = grapqhlExecutor("{value4(k:{alias:\"test\"})}");
        Assertions.assertTrue(executionResult3.getErrors().isEmpty());
        Assertions.assertEquals(4, ((Map) executionResult3.getData()).get("value4"));
    }

}