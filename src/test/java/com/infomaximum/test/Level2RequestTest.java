package com.infomaximum.test;

import com.infomaximum.BaseTest;
import com.infomaximum.cluster.graphql.executor.struct.GExecutionResult;
import graphql.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

public class Level2RequestTest extends BaseTest {

    @Test
    public void test() {
        Assertions.assertTrue(true);

        GExecutionResult executionResult = grapqhlExecutor("{level2{value(k:5)}}");
        Map dataResult = executionResult.getData();

        Assertions.assertEquals(6, ((Map) dataResult.get("level2")).get("value"));
    }

    @Test
    public void testNotNull() {
        GExecutionResult executionResult1 = grapqhlExecutor("{level2{value_not_null(k1: 1, k2: null, k3: 4)}}");
        Map dataResult1 = executionResult1.getData();
        Assertions.assertEquals(5, ((Map) dataResult1.get("level2")).get("value_not_null"));

        GExecutionResult executionResult2 = grapqhlExecutor("{level2{value_not_null(k1: 1, k2: null, k3: null)}}");
        Assert.assertNotEmpty(executionResult2.getErrors());
    }

    @Test
    public void testQuery() {
        Assertions.assertTrue(true);

        GExecutionResult executionResult = grapqhlExecutor("{level2{query_value(k:5)}}");
        //TODO отремонтировать
        Assertions.assertTrue(executionResult.getErrors().isEmpty());

        Map dataResult = executionResult.getData();

        Assertions.assertEquals(6, ((Map) dataResult.get("level2")).get("query_value"));
    }

    @Test
    public void test2() {
        GExecutionResult executionResult = grapqhlExecutor("{level2{value_double(k:5.2)}}");
        Assertions.assertTrue(executionResult.getErrors().isEmpty());

        Assertions.assertEquals(6.2d, ((Map) ((Map) executionResult.getData()).get("level2")).get("value_double"));
    }

    @Test
    public void test3() {
        GExecutionResult executionResult = grapqhlExecutor("{level2{value_big_decimal(k:5.2)}}");
        Assertions.assertTrue(executionResult.getErrors().isEmpty());

        BigDecimal result = (BigDecimal) ((Map) ((Map) executionResult.getData()).get("level2")).get("value_big_decimal");
        Assertions.assertTrue(new BigDecimal("6.2").equals(result));
    }
}