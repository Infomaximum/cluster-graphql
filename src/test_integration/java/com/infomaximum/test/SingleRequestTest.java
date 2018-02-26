package com.infomaximum.test;

import com.infomaximum.BaseTest;
import java.util.Map;
import graphql.ExecutionResult;
import org.junit.Assert;
import org.junit.Test;

public class SingleRequestTest extends BaseTest {

    @Test
    public void test() throws Exception {
        Assert.assertTrue(true);

        ExecutionResult executionResult = grapqhlExecutor("{value}");

        Assert.assertEquals(0, ((Map)executionResult.getData()).get("value"));
    }

}