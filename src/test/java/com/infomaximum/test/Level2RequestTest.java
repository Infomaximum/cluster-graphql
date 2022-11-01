package com.infomaximum.test;

import com.infomaximum.BaseTest;
import com.infomaximum.cluster.graphql.executor.struct.GExecutionResult;
import graphql.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class Level2RequestTest extends BaseTest {

    @Test
    public void test() throws Exception {
        Assertions.assertTrue(true);

        GExecutionResult executionResult = grapqhlExecutor("{level2{value(k:5)}}");
        Map dataResult = executionResult.getData();

        Assertions.assertEquals(6, ((Map) dataResult.get("level2")).get("value"));
    }

    @Test
    public void testNotNull() throws Exception {
        GExecutionResult executionResult1 = grapqhlExecutor("{level2{value_not_null(k1: 1, k2: null, k3: 4)}}");
        Map dataResult1 = executionResult1.getData();
        Assertions.assertEquals(5, ((Map) dataResult1.get("level2")).get("value_not_null"));

        GExecutionResult executionResult2 = grapqhlExecutor("{level2{value_not_null(k1: 1, k2: null, k3: null)}}");
        Assert.assertNotEmpty(executionResult2.getErrors());
    }

    @Test
    public void testQuery() throws Exception {
        Assertions.assertTrue(true);

        GExecutionResult executionResult = grapqhlExecutor("{level2{query_value(k:5)}}");
        Assertions.assertTrue(executionResult.getErrors().isEmpty());

        Map dataResult = executionResult.getData();

        Assertions.assertEquals(6, ((Map) dataResult.get("level2")).get("query_value"));
    }

}