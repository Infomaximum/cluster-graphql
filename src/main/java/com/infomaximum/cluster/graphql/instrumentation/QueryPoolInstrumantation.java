package com.infomaximum.cluster.graphql.instrumentation;

import com.infomaximum.cluster.graphql.remote.graphql.RControllerGraphQL;
import com.infomaximum.cluster.graphql.schema.build.MergeGraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;
import com.infomaximum.cluster.struct.Component;
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

    private final Component component;
    private final GraphQLSchema graphQLSchema;
    private final Map<String, MergeGraphQLTypeOutObject> remoteGraphQLTypeOutObjects;

    private final AtomicLong incrementQueryId;

    public QueryPoolInstrumantation(Component component, GraphQLSchema graphQLSchema, Map<String, MergeGraphQLTypeOutObject> remoteGraphQLTypeOutObjects) {
        this.component = component;
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
            public void onEnd(List<ValidationError> validationErrors, Throwable t) {
                if (!validationErrors.isEmpty()) return;

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

                    Map<Long, Boolean> prepareResource = new HashMap<Long, Boolean>();
                    prepareQuery(
                            parameters.getInstrumentationState(),
                            parent,
                            node,
                            prepareResource
                    );
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

    private void prepareQuery(QueryInstrumentationState queryInstrumentationState, GraphQLObjectType parent, Node node, Map<Long, Boolean> prepareResource) {
        if (node instanceof Field) {
            Field fieldNode = (Field)node;

            MergeGraphQLTypeOutObject mergeGraphQLTypeOutObject = remoteGraphQLTypeOutObjects.get(parent.getName());

            RGraphQLObjectTypeField rGraphQLObjectTypeField = mergeGraphQLTypeOutObject.getFieldByExternalName(fieldNode.getName());

            if (rGraphQLObjectTypeField.queryPool) {
                String requestQueryKey = new StringBuilder()
                        .append(queryInstrumentationState.id)
                        .toString();

                //Собираем какие ресурсы нам необходимы для лока
                RControllerGraphQL rControllerGraphQL = component.getRemotes().getFromSSUuid(rGraphQLObjectTypeField.componentUuid, RControllerGraphQL.class);
                Map<Long, Boolean> prepareResourceRequest = rControllerGraphQL.prepareRequest(
                        requestQueryKey,
                        parent.getName(),
                        rGraphQLObjectTypeField.name
                );
                for (Map.Entry<Long, Boolean> entry: prepareResourceRequest.entrySet()) {
                    prepareResource.merge(entry.getKey(), entry.getValue(), (val1, val2) -> val1 ? val1 : val2);
                }
            }

            for (Node iNode: fieldNode.getChildren()) {
                prepareQuery(queryInstrumentationState, (GraphQLObjectType)parent.getFieldDefinition(fieldNode.getName()).getType(), iNode, prepareResource);
            }
        } else if (node instanceof OperationDefinition) {
            OperationDefinition operationDefinitionNode = (OperationDefinition) node;
            for (Node iNode: operationDefinitionNode.getChildren()) {
                prepareQuery(queryInstrumentationState, parent, iNode, prepareResource);
            }
        } else if (node instanceof SelectionSet) {
            SelectionSet selectionSetNode = (SelectionSet) node;
            for (Node iNode: selectionSetNode.getChildren()) {
                prepareQuery(queryInstrumentationState, parent, iNode, prepareResource);
            }
        }

    }
}
