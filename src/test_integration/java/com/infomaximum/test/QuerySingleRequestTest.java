package com.infomaximum.test;

import com.infomaximum.BaseTest;
import graphql.ExecutionResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class QuerySingleRequestTest extends BaseTest {

    @Test
    public void test() throws Exception {
        Assert.assertTrue(true);

        ExecutionResult executionResult = grapqhlExecutor("{query_value}");

        Assert.assertEquals(1, ((Map)executionResult.getData()).get("query_value"));
    }

}