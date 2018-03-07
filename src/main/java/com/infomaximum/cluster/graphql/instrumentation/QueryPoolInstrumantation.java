package com.infomaximum.cluster.graphql.instrumentation;

import com.infomaximum.cluster.graphql.schema.build.MergeGraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;
import graphql.ExecutionResult;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.NoOpInstrumentation;
import graphql.execution.instrumentation.parameters.*;
import graphql.language.*;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class QueryPoolInstrumantation implements Instrumentation {

    private final GraphQLSchema graphQLSchema;
    private final Map<String, MergeGraphQLTypeOutObject> remoteGraphQLTypeOutObjects;

    private final AtomicLong incrementQueryId;

    public QueryPoolInstrumantation(GraphQLSchema graphQLSchema, Map<String, MergeGraphQLTypeOutObject> remoteGraphQLTypeOutObjects) {
        this.graphQLSchema = graphQLSchema;
        this.remoteGraphQLTypeOutObjects = remoteGraphQLTypeOutObjects;

        this.incrementQueryId = new AtomicLong();
    }

    @Override
    public InstrumentationState createState() {
        return new QueryInstrumentationState(incrementQueryId.incrementAndGet());
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecution(InstrumentationExecutionParameters parameters) {
        return new NoOpInstrumentation.NoOpInstrumentationContext<>();
    }

    @Override
    public InstrumentationContext<Document> beginParse(InstrumentationExecutionParameters parameters) {
        return new NoOpInstrumentation.NoOpInstrumentationContext<>();
    }

    //Валидация запроса - тут лочим все ресурсы
    @Override
    public InstrumentationContext<List<ValidationError>> beginValidation(InstrumentationValidationParameters parameters) {
        return new InstrumentationContext<List<ValidationError>>() {
            @Override
            public void onEnd(List<ValidationError> result, Throwable t) {
                if (!result.isEmpty()) return;

                Document document = parameters.getDocument();
                for (Node node: document.getChildren()) {
                    GraphQLObjectType parent;

                    OperationDefinition operationDefinition = (OperationDefinition) node;
                    if (operationDefinition.getOperation() == OperationDefinition.Operation.QUERY) {
                        parent = graphQLSchema.getQueryType();
                    } else if (operationDefinition.getOperation() == OperationDefinition.Operation.MUTATION) {
                        parent = graphQLSchema.getMutationType();
                    } else if (operationDefinition.getOperation() == OperationDefinition.Operation.SUBSCRIPTION) {
                        parent = graphQLSchema.getSubscriptionType();
                    } else {
                        throw new RuntimeException("not support operation type: " + operationDefinition.getOperation());
                    }

                    prepareQuery(parent, node);
                }

            }
        };
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginDataFetch(InstrumentationDataFetchParameters parameters) {
        return new NoOpInstrumentation.NoOpInstrumentationContext<>();
    }

    @Override
    public InstrumentationContext<CompletableFuture<ExecutionResult>> beginExecutionStrategy(InstrumentationExecutionStrategyParameters parameters) {
        return new NoOpInstrumentation.NoOpInstrumentationContext<>();
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginField(InstrumentationFieldParameters parameters) {
        return new NoOpInstrumentation.NoOpInstrumentationContext<>();
    }

    @Override
    public InstrumentationContext<Object> beginFieldFetch(InstrumentationFieldFetchParameters parameters) {
        return new NoOpInstrumentation.NoOpInstrumentationContext<>();
    }

    //Запрос завершился - возможно с ошибками - поэтому все чистим
    @Override
    public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult, InstrumentationExecutionParameters parameters) {


        return CompletableFuture.completedFuture(executionResult);
    }

    private void prepareQuery(GraphQLObjectType parent, Node node) {
        if (node instanceof Field) {
            Field fieldNode = (Field)node;

            MergeGraphQLTypeOutObject mergeGraphQLTypeOutObject = remoteGraphQLTypeOutObjects.get(parent.getName());

            RGraphQLObjectTypeField rGraphQLObjectTypeField = mergeGraphQLTypeOutObject.getFieldByExternalName(fieldNode.getName());

            if (rGraphQLObjectTypeField.queryPool) {
                //Собираем какие ресурсы нам необходимы для лока
                mergeGraphQLTypeOutObject.getFields();
            }

            for (Node iNode: fieldNode.getChildren()) {
                prepareQuery((GraphQLObjectType)parent.getFieldDefinition(fieldNode.getName()).getType(), iNode);
            }
        } else if (node instanceof OperationDefinition) {
            OperationDefinition operationDefinitionNode = (OperationDefinition) node;
            for (Node iNode: operationDefinitionNode.getChildren()) {
                prepareQuery(parent, iNode);
            }
        } else if (node instanceof SelectionSet) {
            SelectionSet selectionSetNode = (SelectionSet) node;
            for (Node iNode: selectionSetNode.getChildren()) {
                prepareQuery(parent, iNode);
            }
        }

    }
}
