package com.infomaximum.test;

import com.infomaximum.BaseTest;
import com.infomaximum.cluster.graphql.executor.struct.GExecutionResult;
import org.junit.jupiter.api.Test;

public class RequestWithExceptionTest extends BaseTest {

    @Test
    public void test1() throws Exception {
        GExecutionResult executionResult3 = grapqhlExecutor("{value5(k:{alias:\"test\"})}");
        assertGraphQLDataFetchingError(executionResult3, IllegalArgumentException.class);
    }

}