package com.infomaximum.cluster.graphql;

import com.infomaximum.cluster.core.remote.Remotes;
import com.infomaximum.cluster.graphql.customfieldargument.CustomFieldArgument;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.executor.GraphQLExecutor;
import com.infomaximum.cluster.graphql.executor.builder.GraphQLExecutorBuilder;
import com.infomaximum.cluster.graphql.preparecustomfield.PrepareCustomField;
import com.infomaximum.cluster.graphql.remote.graphql.RControllerGraphQLImpl;
import com.infomaximum.cluster.graphql.schema.GraphQLComponentExecutor;
import com.infomaximum.cluster.graphql.schema.build.graphqltype.TypeGraphQLFieldConfigurationBuilder;
import com.infomaximum.cluster.graphql.schema.datafetcher.ComponentDataFetcher;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;
import com.infomaximum.cluster.struct.Component;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Set;

public class GraphQLEngine {

    private final String sdkPackagePath;

    private final Set<PrepareCustomField> prepareCustomFields;
    private final TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder;

    private final Set<CustomFieldArgument> customArguments;
    private final Constructor customRemoteDataFetcher;

    private GraphQLEngine(
            String sdkPackagePath,

            Set<PrepareCustomField> prepareCustomFields,
            TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder,

            Set<CustomFieldArgument> customArguments,

            Constructor customRemoteDataFetcher
            ){

        this.sdkPackagePath = sdkPackagePath;

        this.prepareCustomFields = prepareCustomFields;
        this.fieldConfigurationBuilder = fieldConfigurationBuilder;

        this.customArguments = customArguments;

        this.customRemoteDataFetcher = customRemoteDataFetcher;
    }

    public GraphQLExecutor buildExecutor(Component component) throws GraphQLExecutorException {
        return new GraphQLExecutorBuilder(
                component,
                sdkPackagePath,
                customRemoteDataFetcher,
                prepareCustomFields,
                fieldConfigurationBuilder
        ).build();
    }

    public RControllerGraphQLImpl buildRemoteControllerGraphQL(Component component) throws GraphQLExecutorException {
        return new RControllerGraphQLImpl(component, customArguments, prepareCustomFields, fieldConfigurationBuilder);
    }

    public static class Builder {

        private String sdkPackagePath;

        private Set<PrepareCustomField> prepareCustomFields;
        private TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder;

        private Set<CustomFieldArgument> customArguments;

        private Constructor customRemoteDataFetcher;

        public Builder() {}

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
            Constructor constructor = null;
            try {
                constructor = clazzComponentDataFetcher.getConstructor(Remotes.class, GraphQLComponentExecutor.class, String.class, RGraphQLObjectTypeField.class);
            } catch (NoSuchMethodException e) {
                throw new GraphQLExecutorException("Not found constructor from ComponentDataFetcher", e);
            }
            constructor.setAccessible(true);

            customRemoteDataFetcher = constructor;
            return this;
        }


        public GraphQLEngine build() {
            return new GraphQLEngine(
                    sdkPackagePath,

                    prepareCustomFields,
                    fieldConfigurationBuilder,

                    customArguments,

                    customRemoteDataFetcher
            );
        }
    }
}
