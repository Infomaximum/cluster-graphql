package com.infomaximum.cluster.graphql.executor;

import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorInvalidSyntaxException;
import com.infomaximum.cluster.graphql.preparecustomfield.PrepareCustomField;
import com.infomaximum.cluster.graphql.preparecustomfield.PrepareCustomFieldUtils;
import com.infomaximum.cluster.graphql.remote.graphql.RControllerGraphQL;
import com.infomaximum.cluster.graphql.schema.GraphQLSchemaType;
import com.infomaximum.cluster.graphql.schema.build.MergeGraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.schema.datafetcher.ComponentDataFetcher;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;
import com.infomaximum.cluster.graphql.struct.ContextRequest;
import com.infomaximum.cluster.struct.Component;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.InvalidSyntaxError;
import graphql.execution.ValuesResolver;
import graphql.execution.instrumentation.Instrumentation;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.preparsed.PreparsedDocumentEntry;
import graphql.execution.preparsed.PreparsedDocumentProvider;
import graphql.introspection.Introspection;
import graphql.language.*;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final static Logger log = LoggerFactory.getLogger(GraphQLExecutorPrepareImpl.class);

    @FunctionalInterface
    public interface Function<T extends Serializable> {
        void prepare(T t);
    }

    public static class PrepareDocumentRequest {

        public final ExecutionInput executionInput;
        public final PreparsedDocumentEntry preparsedDocumentEntry;
        public final InstrumentationState instrumentationState;

        public PrepareDocumentRequest(ExecutionInput executionInput, PreparsedDocumentEntry preparsedDocumentEntry, InstrumentationState instrumentationState) {
            this.executionInput = executionInput;
            this.preparsedDocumentEntry = preparsedDocumentEntry;
            this.instrumentationState = instrumentationState;
        }
    }

    private final Component component;
    private final GraphQLSchema schema;
    private final GraphQL graphQL;
    private final GraphQLSchemaType graphQLSchemaType;

    private final Instrumentation instrumentation;
    private final PreparsedDocumentProvider preparsedDocumentProvider;
    private final Method methodParseAndValidate;
    private final Method methodExecute;

    private final Map<String, MergeGraphQLTypeOutObject> remoteGraphQLTypeOutObjects;

    public GraphQLExecutorPrepareImpl(Component component, GraphQLSchema schema, GraphQL graphQL, Map<String, MergeGraphQLTypeOutObject> remoteGraphQLTypeOutObjects, GraphQLSchemaType graphQLSchemaType) {
        this.component = component;
        this.schema = schema;
        this.graphQL = graphQL;
        this.remoteGraphQLTypeOutObjects = remoteGraphQLTypeOutObjects;
        this.graphQLSchemaType = graphQLSchemaType;

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

    public PrepareDocumentRequest prepare(ExecutionInput executionInput, Function fn) throws GraphQLExecutorDataFetcherException {
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

        if (preparsedDocumentEntry.hasErrors()) {
            //Произошла ошибка парсинга
            return new PrepareDocumentRequest(
                    executionInput,
                    preparsedDocumentEntry,
                    instrumentationState
            );
        }

        //Документ распарсен - вызываем prepare
        try {
            Document document = preparsedDocumentEntry.getDocument();
            for (Node node : document.getChildren()) {
                if (node instanceof OperationDefinition) {
                    OperationDefinition operationDefinition = (OperationDefinition) node;

                    GraphQLObjectType parent;
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
                            parent,
                            node,
                            executionInput.getVariables(),
                            fn,
                            (ContextRequest) executionInput.getContext()
                    );
                } else if (node instanceof FragmentDefinition) {
                    FragmentDefinition fragmentDefinition = (FragmentDefinition) node;

                    GraphQLObjectType parent = schema.getObjectType(fragmentDefinition.getTypeCondition().getName());
                    prepareRequest(
                            parent,
                            node,
                            executionInput.getVariables(),
                            fn,
                            (ContextRequest) executionInput.getContext()
                    );
                }
            }

            return new PrepareDocumentRequest(
                    executionInput,
                    preparsedDocumentEntry,
                    instrumentationState
            );
        } catch (GraphQLExecutorInvalidSyntaxException e) {
            //Произошла ошибка парсинга
            return new PrepareDocumentRequest(
                    executionInput,
                    new PreparsedDocumentEntry(new InvalidSyntaxError(
                            new SourceLocation(0, 0),
                            e.getMessage())),
                    instrumentationState
            );
        }
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

    public void prepareException(Throwable throwable, ContextRequest context) {
        //Ulitin V. В будущем необходимо решить вопрос с удаленым вызовым
        for (PrepareCustomField prepareCustomField : graphQLSchemaType.prepareCustomFields) {
            prepareCustomField.prepareException(throwable, context);
        }
    }

    @Override
    public ExecutionResult execute(ExecutionInput executionInput) {
        return graphQL.execute(executionInput);
    }

    @Override
    public void requestCompleted(Throwable ex, ContextRequest context) {
        //Ulitin V. В будущем необходимо решить вопрос с удаленым вызовым
        for (PrepareCustomField prepareCustomField : graphQLSchemaType.prepareCustomFields) {
            prepareCustomField.requestCompleted(ex, context);
        }
    }

    private static String GRAPHQL_TYPE = "__Type";
    private static String GRAPHQL_INPUT_VALUE = "__InputValue";

    private static String GRAPHQL_FIELD_SCHEME = "__schema";
    private static String GRAPHQL_FIELD_TYPENAME = "__typename";

    private void prepareRequest(GraphQLType parent, Node node, Map<String, Object> variables, Function fn, ContextRequest context) throws GraphQLExecutorDataFetcherException {
        if (GRAPHQL_TYPE.equals(parent.getName())) return;
        if (GRAPHQL_INPUT_VALUE.equals(parent.getName())) return;

        if (node instanceof graphql.language.Field) {
            graphql.language.Field field = (graphql.language.Field)node;
            if (GRAPHQL_FIELD_SCHEME.equals(field.getName())) return;
            if (GRAPHQL_FIELD_TYPENAME.equals(field.getName())) return;

            MergeGraphQLTypeOutObject mergeGraphQLTypeOutObject = remoteGraphQLTypeOutObjects.get(parent.getName());
            if (mergeGraphQLTypeOutObject==null) return;

            RGraphQLObjectTypeField rGraphQLObjectTypeField = mergeGraphQLTypeOutObject.getFieldByExternalName(field.getName());

            if (rGraphQLObjectTypeField.isPrepare) {
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
                Serializable prepareRequest = rControllerGraphQL.prepare(
                        PrepareCustomFieldUtils.getKeyField(field),
                        parent.getName(),
                        rGraphQLObjectTypeField.name,
                        arguments,
                        context
                );
                fn.prepare(prepareRequest);
            }

            for (Node iNode: field.getChildren()) {
                if (parent instanceof GraphQLObjectType) {
                    prepareRequest(((GraphQLObjectType) parent).getFieldDefinition(field.getName()).getType(), iNode, variables, fn, context);
                } else if (parent instanceof GraphQLList) {
                    prepareRequest(parent, iNode, variables, fn, context);
                } else {
                    throw new RuntimeException("not support parent type: " + parent);
                }
            }
        } else if (node instanceof SelectionSet) {
            SelectionSet selectionSetNode = (SelectionSet) node;
            for (Node iNode: selectionSetNode.getChildren()) {
                if (parent instanceof GraphQLList) {
                    prepareRequest(((GraphQLList) parent).getWrappedType(), iNode, variables, fn, context);
                } else {
                    prepareRequest(parent, iNode, variables, fn, context);
                }
            }
        } else if (node instanceof OperationDefinition) {
            OperationDefinition operationDefinitionNode = (OperationDefinition) node;
            for (Node iNode : operationDefinitionNode.getChildren()) {
                prepareRequest(parent, iNode, variables, fn, context);
            }
        } else if (node instanceof FragmentDefinition) {
            FragmentDefinition fragmentDefinition = (FragmentDefinition) node;
            for (Node iNode : fragmentDefinition.getChildren()) {
                prepareRequest(parent, iNode, variables, fn, context);
            }
        } else if (node instanceof InlineFragment) {
            InlineFragment inlineFragment = (InlineFragment) node;
            for (Node iNode : inlineFragment.getChildren()) {
                prepareRequest(schema.getObjectType(inlineFragment.getTypeCondition().getName()), iNode, variables, fn, context);
            }
        }
    }
}
