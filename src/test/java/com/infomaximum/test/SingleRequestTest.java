package com.infomaximum.test;

import com.infomaximum.BaseTest;
import com.infomaximum.cluster.graphql.executor.struct.GExecutionResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class SingleRequestTest extends BaseTest {

    @Test
    public void test() throws Exception {
        Assert.assertTrue(true);

        GExecutionResult executionResult = grapqhlExecutor("{value}");
        Assert.assertTrue(executionResult.getErrors().isEmpty());

        Assert.assertEquals(0, ((Map)executionResult.getData()).get("value"));
    }

}