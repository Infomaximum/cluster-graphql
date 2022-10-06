package com.infomaximum.test;

import com.infomaximum.BaseTest;
import com.infomaximum.cluster.graphql.executor.struct.GExecutionResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class SingleRequestTest extends BaseTest {

    @Test
    public void test() throws Exception {
        Assertions.assertTrue(true);

        GExecutionResult executionResult = grapqhlExecutor("{value}");
        Assertions.assertTrue(executionResult.getErrors().isEmpty());

        Assertions.assertEquals(0, ((Map) executionResult.getData()).get("value"));
    }

}