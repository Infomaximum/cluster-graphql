package com.infomaximum.cluster.graphql;

import com.infomaximum.cluster.core.remote.Remotes;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.executor.GraphQLExecutor;
import com.infomaximum.cluster.graphql.executor.builder.GraphQLExecutorBuilder;
import com.infomaximum.cluster.graphql.executor.component.GraphQLComponentExecutor;
import com.infomaximum.cluster.graphql.executor.subscription.GraphQLSubscribeEngine;
import com.infomaximum.cluster.graphql.executor.subscription.GraphQLSubscribeEngineImpl;
import com.infomaximum.cluster.graphql.executor.subscription.GraphQLSubscribeEvent;
import com.infomaximum.cluster.graphql.fieldargument.custom.CustomFieldArgument;
import com.infomaximum.cluster.graphql.preparecustomfield.PrepareCustomField;
import com.infomaximum.cluster.graphql.remote.graphql.executor.RControllerGraphQLExecutorImpl;
import com.infomaximum.cluster.graphql.remote.graphql.subscribe.RControllerGraphQLSubscribeImpl;
import com.infomaximum.cluster.graphql.schema.GraphQLSchemaType;
import com.infomaximum.cluster.graphql.schema.build.graphqltype.TypeGraphQLFieldConfigurationBuilder;
import com.infomaximum.cluster.graphql.schema.datafetcher.ComponentDataFetcher;
import com.infomaximum.cluster.graphql.schema.scalartype.GraphQLScalarTypeCustom;
import com.infomaximum.cluster.graphql.schema.scalartype.GraphQLTypeScalar;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;
import com.infomaximum.cluster.struct.Component;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GraphQLEngine {

    private final String sdkPackagePath;

    private final TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder;

    private final GraphQLSchemaType graphQLSchemaType;

    private final Constructor customRemoteDataFetcher;

    private GraphQLEngine(
            String sdkPackagePath,

            TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder,

            GraphQLSchemaType graphQLSchemaType,

            Constructor customRemoteDataFetcher
            ){

        this.sdkPackagePath = sdkPackagePath;

        this.fieldConfigurationBuilder = fieldConfigurationBuilder;

        this.graphQLSchemaType = graphQLSchemaType;

        this.customRemoteDataFetcher = customRemoteDataFetcher;
    }

    public GraphQLSchemaType getGraphQLSchemaType() {
        return graphQLSchemaType;
    }

    public GraphQLSubscribeEngine buildSubscribeEngine() {
        return new GraphQLSubscribeEngineImpl();
    }

    public GraphQLExecutor buildExecutor(Component component, GraphQLSubscribeEngine graphQLSubscribeEngine) throws GraphQLExecutorException {
        return new GraphQLExecutorBuilder(
                component,
                sdkPackagePath,
                customRemoteDataFetcher,
                fieldConfigurationBuilder,
                graphQLSchemaType,
                (GraphQLSubscribeEngineImpl) graphQLSubscribeEngine
        ).build();
    }

    public RControllerGraphQLSubscribeImpl buildRemoteControllerGraphQLSubscribe(Component component, GraphQLSubscribeEngine graphQLSubscribeEngine) throws GraphQLExecutorException {
        return new RControllerGraphQLSubscribeImpl(component, (GraphQLSubscribeEngineImpl) graphQLSubscribeEngine);
    }

    public RControllerGraphQLExecutorImpl buildRemoteControllerGraphQLExecutor(Component component) throws GraphQLExecutorException {
        return new RControllerGraphQLExecutorImpl(component, fieldConfigurationBuilder, graphQLSchemaType);
    }

    public GraphQLSubscribeEvent buildSubscribeEvent(Component component) {
        return new GraphQLSubscribeEvent(component);
    }

    public static class Builder {

        private String sdkPackagePath;

        private Set<PrepareCustomField> prepareCustomFields;
        private TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder;

        private Set<CustomFieldArgument> customArguments;

        private Constructor customRemoteDataFetcher;

        private Set<GraphQLTypeScalar> typeScalars;

        public Builder() {
            typeScalars = new HashSet<GraphQLTypeScalar>();
            typeScalars.add(GraphQLScalarTypeCustom.GraphQLBoolean);
            typeScalars.add(GraphQLScalarTypeCustom.GraphQLString);
            typeScalars.add(GraphQLScalarTypeCustom.GraphQLInt);
            typeScalars.add(GraphQLScalarTypeCustom.GraphQLLong);
            typeScalars.add(GraphQLScalarTypeCustom.GraphQLBigDecimal);
            typeScalars.add(GraphQLScalarTypeCustom.GraphQLFloat);
            typeScalars.add(GraphQLScalarTypeCustom.GraphQLInstant);
        }

        public Builder withSDKPackage(Package sdkPackage) {
            this.sdkPackagePath = sdkPackage.getName();
            return this;
        }

        public Builder withSDKPackage(String sdkPackagePath) {
            this.sdkPackagePath = sdkPackagePath;
            return this;
        }

        public Builder withPrepareCustomField(PrepareCustomField prepareCustomField) {
            if (prepareCustomFields ==null) prepareCustomFields = new HashSet<>();
            prepareCustomFields.add(prepareCustomField);
            return this;
        }

        public Builder withFieldConfigurationBuilder(TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder){
            this.fieldConfigurationBuilder = fieldConfigurationBuilder;
            return this;
        }

        public Builder withCustomArgument(CustomFieldArgument customArgument) {
            if (customArguments==null) customArguments = new HashSet<>();
            customArguments.add(customArgument);
            return this;
        }

        public Builder withDataFetcher(Class<? extends ComponentDataFetcher> clazzComponentDataFetcher) throws GraphQLExecutorException {
            Constructor constructor;
            try {
                constructor = clazzComponentDataFetcher.getConstructor(Remotes.class, GraphQLComponentExecutor.class, GraphQLSubscribeEngineImpl.class, String.class, RGraphQLObjectTypeField.class);
            } catch (NoSuchMethodException e) {
                throw new GraphQLExecutorException("Not found constructor from ComponentDataFetcher", e);
            }
            constructor.setAccessible(true);

            customRemoteDataFetcher = constructor;
            return this;
        }

        public Builder withTypeScalar(GraphQLTypeScalar typeScalar) {
            typeScalars.add(typeScalar);
            return this;
        }

        public GraphQLEngine build() {
            return new GraphQLEngine(
                    sdkPackagePath,

                    fieldConfigurationBuilder,

                    new GraphQLSchemaType(
                            typeScalars,
                            (prepareCustomFields == null) ? Collections.emptySet() : prepareCustomFields,
                            (customArguments == null) ? Collections.emptySet() : customArguments
                    ),

                    customRemoteDataFetcher
            );
        }
    }
}
