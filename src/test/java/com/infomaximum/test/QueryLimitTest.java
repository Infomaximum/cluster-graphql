package com.infomaximum.test;

import com.infomaximum.BaseTest;
import com.infomaximum.cluster.graphql.GraphQLEngine;
import com.infomaximum.cluster.graphql.executor.GraphQLExecutor;
import com.infomaximum.cluster.graphql.executor.GraphQLExecutorImpl;
import com.infomaximum.cluster.graphql.executor.GraphQLExecutorPrepareImpl;
import com.infomaximum.cluster.graphql.executor.struct.GExecutionResult;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.server.components.frontend.FrontendComponent;
import graphql.ExecutionInput;
import graphql.GraphQLError;
import graphql.execution.ExecutionId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Проверяет лимиты GraphQL по глубине (depth=30) и сложности (complexity=1000)
 * на обоих путях исполнения: регулярном ({@link GraphQLExecutorImpl}) и
 * prepared ({@link GraphQLExecutorPrepareImpl}.
 */
public class QueryLimitTest extends BaseTest {

    /** Запрос глубины {@code childCount + 2} (recursive → childCount×child → value). */
    private static String nestedQuery(int childCount) {
        StringBuilder sb = new StringBuilder("{recursive");
        for (int i = 0; i < childCount; i++) {
            sb.append("{child");
        }
        sb.append("{value}");
        for (int i = 0; i < childCount; i++) {
            sb.append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    /** Запрос с {@code fieldCount} алиасами скалярного поля value (сложность = fieldCount). */
    private static String wideQuery(int fieldCount) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < fieldCount; i++) {
            sb.append("a").append(i).append(":value ");
        }
        sb.append("}");
        return sb.toString();
    }

    private static ExecutionInput executionInput(String query) {
        GRequest gRequest = new GRequest(
                Instant.now(),
                new GRequest.RemoteAddress("127.0.0.1"),
                "{}", new HashMap<>(), null,
                "123e4567-e89b-12d3-a456-426655440000"
        );
        return ExecutionInput.newExecutionInput()
                .query(query)
                .executionId(ExecutionId.generate())
                .context(new TestContextRequest(gRequest))
                .variables(Collections.emptyMap())
                .build();
    }

    /** Регулярный путь — отдельный движок без prepareCustomField. */
    private static GraphQLExecutor regularExecutor() throws Exception {
        FrontendComponent frontendComponent = getServer().getCluster().getAnyLocalComponent(FrontendComponent.class);
        GraphQLEngine regularEngine = new GraphQLEngine.Builder().build();
        GraphQLExecutor executor = regularEngine.buildExecutor(frontendComponent, regularEngine.buildSubscribeEngine());
        Assertions.assertInstanceOf(GraphQLExecutorImpl.class, executor);
        return executor;
    }

    /** Boevой prepared-путь: prepare() + execute(PrepareDocumentRequest). */
    private static GExecutionResult executePrepared(String query) throws Exception {
        FrontendComponent frontendComponent = getServer().getCluster().getAnyLocalComponent(FrontendComponent.class);
        GraphQLExecutorPrepareImpl executor = (GraphQLExecutorPrepareImpl) frontendComponent.getGraphQLExecutor();
        GraphQLExecutorPrepareImpl.PrepareDocumentRequest request = executor.prepare(
                executionInput(query),
                (rGraphQLObjectTypeField, prepare) -> { /* no-op: тестовые поля не требуют lock-ресурсов */ }
        );
        return executor.execute(request);
    }

    private static void assertLimitError(GExecutionResult result, String expectedFragment) {
        List<GraphQLError> errors = result.getErrors();
        Assertions.assertFalse(errors.isEmpty(), "ожидалась ошибка превышения лимита, errors пуст");
        boolean matched = errors.stream()
                .anyMatch(e -> e.getMessage() != null && e.getMessage().toLowerCase().contains(expectedFragment));
        Assertions.assertTrue(matched,
                "ожидалось сообщение содержащее '" + expectedFragment + "', получено: " + errors);
    }

    // ---- Регулярный путь (GraphQLExecutorImpl) ----

    /** Запрос глубиной 42 (> лимита 30) на регулярном пути отклоняется ошибкой превышения глубины. */
    @Test
    public void regularPathRejectsDeepQuery() throws Exception {
        GExecutionResult result = regularExecutor().execute(executionInput(nestedQuery(40)));
        assertLimitError(result, "maximum query depth exceeded");
    }

    /** Запрос с 1001 полем (> лимита 1000) на регулярном пути отклоняется ошибкой превышения сложности. */
    @Test
    public void regularPathRejectsComplexQuery() throws Exception {
        GExecutionResult result = regularExecutor().execute(executionInput(wideQuery(1001)));
        assertLimitError(result, "maximum query complexity exceeded");
    }

    // ---- Prepared путь (GraphQLExecutorPrepareImpl, prepare + execute) ----

    /** Запрос глубиной 32 на боевом prepared-пути отклоняется ошибкой в errors[], без проброса исключения. */
    @Test
    public void preparedPathRejectsDeepQuery() throws Exception {
        GExecutionResult result = executePrepared(nestedQuery(30));
        assertLimitError(result, "maximum query depth exceeded");
    }

    /** Запрос с 1001 полем на боевом prepared-пути отклоняется ошибкой в errors[], без проброса исключения. */
    @Test
    public void preparedPathRejectsComplexQuery() throws Exception {
        GExecutionResult result = executePrepared(wideQuery(1001));
        assertLimitError(result, "maximum query complexity exceeded");
    }

    // ---- Граница: запрос в пределах лимитов проходит (оба пути) ----

    /** Запрос глубиной ровно 30 (на границе лимита) на регулярном пути выполняется без ошибок. */
    @Test
    public void regularPathAllowsQueryAtDepthLimit() throws Exception {
        GExecutionResult result = regularExecutor().execute(executionInput(nestedQuery(28)));
        Assertions.assertTrue(result.getErrors().isEmpty(), "запрос глубины 30 не должен отклоняться: " + result.getErrors());
    }

    /** Запрос с ровно 1000 полями (на границе лимита) на регулярном пути выполняется без ошибок. */
    @Test
    public void regularPathAllowsQueryAtComplexityLimit() throws Exception {
        GExecutionResult result = regularExecutor().execute(executionInput(wideQuery(1000)));
        Assertions.assertTrue(result.getErrors().isEmpty(), "запрос сложности 1000 не должен отклоняться: " + result.getErrors());
    }

    /** Запрос глубиной ровно 30 на боевом prepared-пути выполняется без ошибок. */
    @Test
    public void preparedPathAllowsQueryWithinLimits() throws Exception {
        GExecutionResult result = executePrepared(nestedQuery(28));
        Assertions.assertTrue(result.getErrors().isEmpty(), "запрос глубины 30 не должен отклоняться: " + result.getErrors());
    }

    /** Запрос с ровно 1000 полями на боевом prepared-пути выполняется без ошибок. */
    @Test
    public void preparedPathAllowsQueryAtComplexityLimit() throws Exception {
        GExecutionResult result = executePrepared(wideQuery(1000));
        Assertions.assertTrue(result.getErrors().isEmpty(), "запрос сложности 1000 не должен отклоняться: " + result.getErrors());
    }
}
