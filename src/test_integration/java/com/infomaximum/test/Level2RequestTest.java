package com.infomaximum.test;

import com.infomaximum.BaseTest;
import graphql.ExecutionResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class Level2RequestTest extends BaseTest {

    @Test
    public void test() throws Exception {
        Assert.assertTrue(true);

        ExecutionResult executionResult = grapqhlExecutor("{level2{value(k:5)}}");
        Map dataResult = executionResult.getData();

        Assert.assertEquals(6, ((Map)dataResult.get("level2")).get("value"));
    }

    @Test
    public void testQuery() throws Exception {
        Assert.assertTrue(true);

        ExecutionResult executionResult = grapqhlExecutor("{level2{query_value(k:5)}}");
        Map dataResult = executionResult.getData();

        Assert.assertEquals(6, ((Map)dataResult.get("level2")).get("query_value"));
    }

}