package com.infomaximum.cluster.graphql.executor;

import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.remote.graphql.RControllerGraphQL;
import com.infomaximum.cluster.graphql.schema.build.MergeGraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.schema.datafetcher.ComponentDataFetcher;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.cluster.struct.Component;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.ValuesResolver;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.introspection.Introspection;
import graphql.language.Document;
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Основная идея это разрезать метод parseValidateAndExecute на 2 части и черех грязные хаки вызвать их отдельно
 *
 private CompletableFuture<ExecutionResult> parseValidateAndExecute(ExecutionInput executionInput, GraphQLSchema graphQLSchema, InstrumentationState instrumentationState) {
 PreparsedDocumentEntry preparsedDoc = preparsedDocumentProvider.get(executionInput.getQuery(), query -> parseAndValidate(executionInput, graphQLSchema, instrumentationState));

 if (preparsedDoc.hasErrors()) {
 return CompletableFuture.completedFuture(new ExecutionResultImpl(preparsedDoc.getErrors()));
 }

 return execute(executionInput, preparsedDoc.getDocument(), graphQLSchema, instrumentationState);
 }
 *
 *
 */
public class GraphQLExecutorPrepareImpl implements GraphQLExecutor {

    public static class PrepareDocumentRequest {

        private final ExecutionInput executionInput;
        private final PreparsedDocumentEntry preparsedDocumentEntry;
        private final InstrumentationState instrumentationState;

        public PrepareDocumentRequest(ExecutionInput executionInput, PreparsedDocumentEntry preparsedDocumentEntry, InstrumentationState instrumentationState) {
            this.executionInput = executionInput;
            this.preparsedDocumentEntry = preparsedDocumentEntry;
            this.instrumentationState = instrumentationState;
        }
    }

    private final Component component;
    private final GraphQLSchema schema;
    private final GraphQL graphQL;

    private final Instrumentation instrumentation;
    private final PreparsedDocumentProvider preparsedDocumentProvider;
    private final Method methodParseAndValidate;
    private final Method methodExecute;

    private final Map<String, MergeGraphQLTypeOutObject> remoteGraphQLTypeOutObjects;

    public GraphQLExecutorPrepareImpl(Component component, GraphQLSchema schema, GraphQL graphQL, Map<String, MergeGraphQLTypeOutObject> remoteGraphQLTypeOutObjects) {
        this.component = component;
        this.schema = schema;
        this.graphQL = graphQL;
        this.remoteGraphQLTypeOutObjects = remoteGraphQLTypeOutObjects;

        try {
            Field fieldInstrumentation = graphQL.getClass().getDeclaredField("instrumentation");
            fieldInstrumentation.setAccessible(true);
            instrumentation = (Instrumentation) fieldInstrumentation.get(graphQL);

            Field fieldPreparsedDocumentProvider = graphQL.getClass().getDeclaredField("preparsedDocumentProvider");
            fieldPreparsedDocumentProvider.setAccessible(true);
            preparsedDocumentProvider = (PreparsedDocumentProvider) fieldPreparsedDocumentProvider.get(graphQL);

            methodParseAndValidate = graphQL.getClass().getDeclaredMethod("parseAndValidate", ExecutionInput.class, GraphQLSchema.class, InstrumentationState.class);
            methodParseAndValidate.setAccessible(true);

            methodExecute = graphQL.getClass().getDeclaredMethod("execute", ExecutionInput.class, Document.class, GraphQLSchema.class, InstrumentationState.class);
            methodExecute.setAccessible(true);

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Изменилась реализация библиотеки GraphQL", e);
        }
    }

    public GraphQLSchema getSchema() {
        return schema;
    }

    public PrepareDocumentRequest prepare(ExecutionInput executionInput) throws GraphQLExecutorDataFetcherException {
        InstrumentationState instrumentationState = instrumentation.createState();

        InstrumentationExecutionParameters inputInstrumentationParameters = new InstrumentationExecutionParameters(executionInput, schema, instrumentationState);
        executionInput = instrumentation.instrumentExecutionInput(executionInput, inputInstrumentationParameters);

        InstrumentationExecutionParameters instrumentationParameters = new InstrumentationExecutionParameters(executionInput, schema, instrumentationState);
        instrumentation.beginExecution(instrumentationParameters);
        instrumentation.instrumentSchema(schema, instrumentationParameters);


        ExecutionInput finalExecutionInput = executionInput;
        PreparsedDocumentEntry preparsedDocumentEntry = preparsedDocumentProvider.get(executionInput.getQuery(), query -> {
            try {
                return (PreparsedDocumentEntry)methodParseAndValidate.invoke(graphQL, finalExecutionInput, schema, instrumentationState);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Изменилась реализация библиотеки GraphQL", e);
            }
        });


        //Документ распарсен - вызываем prepare
        Document document = preparsedDocumentEntry.getDocument();
        for (Node node: document.getChildren()) {
            if (!(node instanceof OperationDefinition)) continue;

            GraphQLObjectType parent;

            OperationDefinition operationDefinition = (OperationDefinition) node;
            if (operationDefinition.getOperation() == OperationDefinition.Operation.QUERY) {
                parent = schema.getQueryType();
            } else if (operationDefinition.getOperation() == OperationDefinition.Operation.MUTATION) {
                parent = schema.getMutationType();
            } else if (operationDefinition.getOperation() == OperationDefinition.Operation.SUBSCRIPTION) {
                parent = schema.getSubscriptionType();
            } else {
                throw new RuntimeException("not support operation type: " + operationDefinition.getOperation());
            }

            prepareRequest(
                    (GRequest) executionInput.getContext(),
                    parent,
                    node,
                    executionInput.getVariables()
            );
        }

        return new PrepareDocumentRequest(
                executionInput,
                preparsedDocumentEntry,
                instrumentationState
        );
    }

    public ExecutionResult execute(PrepareDocumentRequest prepareDocumentRequest) {
        try {
            CompletableFuture<ExecutionResult> completableFuture = (CompletableFuture<ExecutionResult>) methodExecute.invoke(graphQL,
                    prepareDocumentRequest.executionInput,
                    prepareDocumentRequest.preparsedDocumentEntry.getDocument(),
                    schema,
                    prepareDocumentRequest.instrumentationState);

            return completableFuture.join();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Изменилась реализация библиотеки GraphQL", e);
        }
    }


    @Override
    public ExecutionResult execute(ExecutionInput executionInput) {
        return graphQL.execute(executionInput);
    }


    private static String GRAPHQL_FIELD_SCHEME = "__schema";
    private static String GRAPHQL_FIELD_TYPENAME = "__typename";

    private void prepareRequest(GRequest request, GraphQLType parent, Node node, Map<String, Object> variables) throws GraphQLExecutorDataFetcherException {
        if (node instanceof graphql.language.Field) {
            graphql.language.Field field = (graphql.language.Field)node;
            if (GRAPHQL_FIELD_SCHEME.equals(field.getName())) return;
            if (GRAPHQL_FIELD_TYPENAME.equals(field.getName())) return;

            MergeGraphQLTypeOutObject mergeGraphQLTypeOutObject = remoteGraphQLTypeOutObjects.get(parent.getName());
            if (mergeGraphQLTypeOutObject==null) return;

            RGraphQLObjectTypeField rGraphQLObjectTypeField = mergeGraphQLTypeOutObject.getFieldByExternalName(field.getName());

            if (rGraphQLObjectTypeField.isPrepare) {
                String requestItemKey = "";

                HashMap<String, Serializable> arguments = ComponentDataFetcher.filterArguments(
                        field,
                        new ValuesResolver().getArgumentValues(
                                schema.getFieldVisibility(),
                                Introspection.getFieldDef(schema, (GraphQLObjectType)parent, field.getName()).getArguments(),
                                field.getArguments(),
                                variables
                        ),
                        variables.keySet()
                );

                //Собираем какие ресурсы нам необходимы для лока
                RControllerGraphQL rControllerGraphQL = component.getRemotes().getFromSSUuid(rGraphQLObjectTypeField.componentUuid, RControllerGraphQL.class);
                Serializable prepareRequest = rControllerGraphQL.prepareExecute(
                        requestItemKey,
                        request,
                        parent.getName(),
                        rGraphQLObjectTypeField.name,
                        arguments
                );
//                for (Map.Entry<Long, Boolean> entry: prepareResourceRequest.entrySet()) {
//                    prepareResource.merge(entry.getKey(), entry.getValue(), (val1, val2) -> val1 ? val1 : val2);
//                }
            }

            for (Node iNode: field.getChildren()) {
                try {
                    prepareRequest(request, ((GraphQLObjectType)parent).getFieldDefinition(field.getName()).getType(), iNode, variables);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (node instanceof OperationDefinition) {
            OperationDefinition operationDefinitionNode = (OperationDefinition) node;
            for (Node iNode: operationDefinitionNode.getChildren()) {
                prepareRequest(request, parent, iNode, variables);
            }
        } else if (node instanceof SelectionSet) {
            SelectionSet selectionSetNode = (SelectionSet) node;
            for (Node iNode: selectionSetNode.getChildren()) {
                prepareRequest(request, parent, iNode, variables);
            }
        }

    }
}
