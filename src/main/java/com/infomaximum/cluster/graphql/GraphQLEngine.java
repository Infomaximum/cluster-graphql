package com.infomaximum.cluster.graphql;

import com.infomaximum.cluster.core.remote.AbstractRController;
import com.infomaximum.cluster.core.remote.Remotes;
import com.infomaximum.cluster.graphql.customtype.CustomEnvType;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.executor.GraphQLExecutor;
import com.infomaximum.cluster.graphql.remote.graphql.RControllerGraphQLImpl;
import com.infomaximum.cluster.graphql.schema.GraphQLComponentExecutor;
import com.infomaximum.cluster.graphql.schema.build.graphqltype.TypeGraphQLFieldConfigurationBuilder;
import com.infomaximum.cluster.graphql.schema.datafetcher.ComponentDataFetcher;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;
import com.infomaximum.cluster.querypool.QueryPoolExecutor;
import com.infomaximum.cluster.struct.Component;

import java.lang.reflect.Constructor;
import java.util.Set;

public class GraphQLEngine {

    private final String sdkPackagePath;
    private final Set<CustomEnvType> customEnvTypes;
    private final Constructor customRemoteDataFetcher;
    private final TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder;
    private final QueryPoolExecutor queryPoolExecutor;

    private GraphQLEngine(
            String sdkPackagePath,
            Set<CustomEnvType> customEnvTypes,
            Constructor customRemoteDataFetcher,
            TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder,
            QueryPoolExecutor queryPoolExecutor
            ){

        this.sdkPackagePath = sdkPackagePath;
        this.customEnvTypes = customEnvTypes;
        this.customRemoteDataFetcher = customRemoteDataFetcher;
        this.fieldConfigurationBuilder = fieldConfigurationBuilder;
        this.queryPoolExecutor = queryPoolExecutor;
    }

    public GraphQLExecutor buildExecutor(Component component) throws GraphQLExecutorException {
        return new GraphQLExecutor.Builder(
                component,
                sdkPackagePath,
                customRemoteDataFetcher,
                fieldConfigurationBuilder,
                queryPoolExecutor
        ).build();
    }

    public RControllerGraphQLImpl buildRemoteControllerGraphQL(Component component) throws GraphQLExecutorException {
        try {
            return new RControllerGraphQLImpl(component, customEnvTypes, fieldConfigurationBuilder, queryPoolExecutor);
        } catch (ReflectiveOperationException e) {
            throw new GraphQLExecutorException(e);
        }
    }

    public static class Builder {

        private String sdkPackagePath;
        private Set<CustomEnvType> customEnvTypes;
        private Constructor customRemoteDataFetcher;
        private TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder;
        private QueryPoolExecutor queryPoolExecutor;

        public Builder() {}

        public Builder withSDKPackage(Package sdkPackage) {
            this.sdkPackagePath = sdkPackage.getName();
            return this;
        }

        public Builder withSDKPackage(String sdkPackage) {
            this.sdkPackagePath = sdkPackagePath;
            return this;
        }

        public Builder withCustomEnvTypes(Set<CustomEnvType> customEnvTypes) {
            this.customEnvTypes=customEnvTypes;
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

        public Builder withFieldConfigurationBuilder(TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder){
            this.fieldConfigurationBuilder = fieldConfigurationBuilder;
            return this;
        }

        public Builder withQueryPoolExecutor(QueryPoolExecutor queryPoolExecutor){
            this.queryPoolExecutor = queryPoolExecutor;
            return this;
        }

        public GraphQLEngine build() {
            return new GraphQLEngine(
                    sdkPackagePath,
                    customEnvTypes,
                    customRemoteDataFetcher,
                    fieldConfigurationBuilder,
                    queryPoolExecutor
            );
        }
    }
}
