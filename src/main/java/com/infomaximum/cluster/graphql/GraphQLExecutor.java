package com.infomaximum.cluster.graphql;


import com.infomaximum.cluster.core.remote.Remotes;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.remote.graphql.RControllerGraphQL;
import com.infomaximum.cluster.graphql.scalartype.GraphQLScalarTypeCustom;
import com.infomaximum.cluster.graphql.schema.GraphQLComponentExecutor;
import com.infomaximum.cluster.graphql.schema.TypeSchema;
import com.infomaximum.cluster.graphql.schema.build.MergeGraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.schema.build.MergeGraphQLTypeOutObjectUnion;
import com.infomaximum.cluster.graphql.schema.build.graphqltype.TypeGraphQLFieldConfigurationBuilder;
import com.infomaximum.cluster.graphql.schema.datafetcher.ComponentDataFetcher;
import com.infomaximum.cluster.graphql.schema.datafetcher.ExtPropertyDataFetcher;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLTypeEnum;
import com.infomaximum.cluster.graphql.schema.struct.input.RGraphQLInputObjectTypeField;
import com.infomaximum.cluster.graphql.schema.struct.input.RGraphQLTypeInObject;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLTypeOutObjectUnion;
import com.infomaximum.cluster.graphql.schema.struct.output.RGraphQLObjectTypeField;
import com.infomaximum.cluster.graphql.schema.struct.output.RGraphQLObjectTypeMethodArgument;
import com.infomaximum.cluster.graphql.schema.struct.output.RGraphQLTypeOutObject;
import com.infomaximum.cluster.struct.Component;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.TypeResolutionEnvironment;
import graphql.schema.*;

import java.lang.reflect.Constructor;
import java.util.*;

import static graphql.schema.GraphQLSchema.newSchema;

public class GraphQLExecutor {

    private Component component;
    private GraphQLSchema schema;
    private GraphQL graphQL;

    private GraphQLExecutor(Component component, GraphQLSchema schema, GraphQL graphQL) {
        this.component = component;
        this.schema = schema;
        this.graphQL = graphQL;
    }

    public GraphQLSchema getSchema() {
        return schema;
    }

    public GraphQL getGraphQL() {
        return graphQL;
    }

    public static class Builder {

        private Component component;
        private String environmentPackageName;
        private Constructor customRemoteDataFetcher;
        private TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder;

        //Собираем какие типы у нас вообще есть
        private GraphQLComponentExecutor sdkGraphQLItemExecutor;
        private List<RGraphQLTypeEnum> waitBuildGraphQLTypeEnums = new ArrayList<RGraphQLTypeEnum>();
        private Map<String, MergeGraphQLTypeOutObject> waitBuildGraphQLTypeOutObjects = new HashMap<String, MergeGraphQLTypeOutObject>();
        private Map<String, MergeGraphQLTypeOutObjectUnion> waitBuildGraphQLTypeOutObjectUnions = new HashMap<String, MergeGraphQLTypeOutObjectUnion>();
        private Map<String, Set<RGraphQLInputObjectTypeField>> waitBuildGraphQLTypeInObjects = new HashMap<String, Set<RGraphQLInputObjectTypeField>>();

        public Builder(Component component) {
            this(component, (String) null);
        }

        public Builder(Component component, Package environmentPackage) {
            this.component = component;
            this.environmentPackageName = environmentPackage.getName();
        }

        public Builder(Component component, String environmentPackageName) {
            this.component = component;
            this.environmentPackageName = environmentPackageName;
        }

        public Builder withDataFetcher(Class<? extends ComponentDataFetcher> clazzComponentDataFetcher) throws GraphQLExecutorException {
            Constructor constructor = null;
            try {
                constructor = clazzComponentDataFetcher.getConstructor(Remotes.class, String.class, RGraphQLObjectTypeField.class);
            } catch (NoSuchMethodException e) {
                throw new GraphQLExecutorException("Not found constructor from ComponentDataFetcher", e);
            }
            constructor.setAccessible(true);

            customRemoteDataFetcher = constructor;
            return this;
        }

        public Builder withFieldConfigurationBuilder(TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder){
            this.fieldConfigurationBuilder=fieldConfigurationBuilder;
            return this;
        }

        public GraphQLExecutor build() throws GraphQLExecutorException {
            try {
                //Собираем встроенные
                if (environmentPackageName!=null) {
                    sdkGraphQLItemExecutor = new GraphQLComponentExecutor(environmentPackageName, fieldConfigurationBuilder);
                    for (RGraphQLType rGraphQLType : sdkGraphQLItemExecutor.getCustomTypes()) {
                        mergeGraphQLType(rGraphQLType);
                    }
                }

                //Запрашиваем у подсистем
                Collection<RControllerGraphQL> rControllerGraphQLs = component.getRemotes().getControllers(RControllerGraphQL.class);
                for (RControllerGraphQL rControllerGraphQL : rControllerGraphQLs) {
                    for (RGraphQLType rGraphQLType : rControllerGraphQL.getCustomTypes()) {
                        mergeGraphQLType(rGraphQLType);
                    }
                }

                //В этот map добавляются все построенные типы
                Map<String, GraphQLType> graphQLTypes = new HashMap<String, GraphQLType>();
                graphQLTypes.put("boolean", Scalars.GraphQLBoolean);
                graphQLTypes.put("collection:boolean", new GraphQLList(Scalars.GraphQLBoolean));
                graphQLTypes.put("int", Scalars.GraphQLInt);
                graphQLTypes.put("collection:int", new GraphQLList(Scalars.GraphQLInt));
                graphQLTypes.put("long", Scalars.GraphQLLong);
                graphQLTypes.put("collection:long", new GraphQLList(Scalars.GraphQLLong));
                graphQLTypes.put("bigdecimal", Scalars.GraphQLBigDecimal);
                graphQLTypes.put("collection:bigdecimal", new GraphQLList(Scalars.GraphQLBigDecimal));
                graphQLTypes.put("float", GraphQLScalarTypeCustom.GraphQLFloat);
                graphQLTypes.put("collection:float", new GraphQLList(GraphQLScalarTypeCustom.GraphQLFloat));
                graphQLTypes.put("string", Scalars.GraphQLString);
                graphQLTypes.put("collection:string", new GraphQLList(Scalars.GraphQLString));
                graphQLTypes.put("date", GraphQLScalarTypeCustom.GraphQLDate);
                graphQLTypes.put("collection:date", new GraphQLList(GraphQLScalarTypeCustom.GraphQLDate));

                //Добавляем все enum
                for (RGraphQLTypeEnum rGraphQLEnumType : waitBuildGraphQLTypeEnums) {
                    buildGraphQLTypeEnum(graphQLTypes, rGraphQLEnumType.getName(), rGraphQLEnumType.getEnumValues());
                }

                //Разбираемся с зависимостями input объектами
                for (Map.Entry<String, Set<RGraphQLInputObjectTypeField>> entry : waitBuildGraphQLTypeInObjects.entrySet()) {
                    String graphQLTypeName = entry.getKey();
                    Set<RGraphQLInputObjectTypeField> graphQLTypeFields = entry.getValue();

                    buildGraphQLTypeInObject(graphQLTypes, graphQLTypeName, graphQLTypeFields);
                }


                //Разбираемся с зависимостями output объектов
                while (!waitBuildGraphQLTypeOutObjects.isEmpty()) {
                    boolean isCyclicDependency = true;
                    for (MergeGraphQLTypeOutObject graphQLTypeOutObject : waitBuildGraphQLTypeOutObjects.values()) {

                        boolean isLoadedDependenciesType = true;
                        for (RGraphQLObjectTypeField typeGraphQLField : graphQLTypeOutObject.getFields()) {
                            String[] compositeTypes = typeGraphQLField.type.split(":");
                            for (String compositeType : compositeTypes) {
                                if ("collection".equals(compositeType)) continue;
                                if (!graphQLTypes.containsKey(compositeType)) {
                                    isLoadedDependenciesType = false;
                                }
                            }
                        }

                        if (isLoadedDependenciesType) {
                            //Все зависимости-поля есть, можно загружать
                            buildGraphQLTypeOutObject(graphQLTypes, graphQLTypeOutObject);

                            //Загрузка прошла успешно
                            waitBuildGraphQLTypeOutObjects.remove(graphQLTypeOutObject.name);
                            isCyclicDependency = false;
                            break;
                        }
                    }

                    if (isCyclicDependency) {
                        //Блински циклическая зависимость, строим первую попавшую через ссылки
                        String graphQLTypeName = waitBuildGraphQLTypeOutObjects.keySet().iterator().next();
                        MergeGraphQLTypeOutObject graphQLTypeOutObject = waitBuildGraphQLTypeOutObjects.get(graphQLTypeName);

                        buildGraphQLTypeOutObject(graphQLTypes, graphQLTypeOutObject);

                        //Загрузка прошла успешно
                        waitBuildGraphQLTypeOutObjects.remove(graphQLTypeName);
                    }
                }

                //Разбираемся с зависимостями output union объектов
                for (MergeGraphQLTypeOutObjectUnion mergeGraphQLTypeOutObjectUnion : waitBuildGraphQLTypeOutObjectUnions.values()) {
                    buildGraphQLTypeOutObjectUnion(graphQLTypes, mergeGraphQLTypeOutObjectUnion);
                }


                GraphQLSchema schema = newSchema()
                        .query((GraphQLObjectType) graphQLTypes.get(TypeSchema.QUERY.getValue()))
                        .mutation((GraphQLObjectType) graphQLTypes.get(TypeSchema.MUTATION.getValue()))
                        .build(new HashSet<GraphQLType>(graphQLTypes.values()));

                GraphQL graphQL = GraphQL.newGraphQL(schema).build();

                return new GraphQLExecutor(component, schema, graphQL);
            } catch (Throwable throwable) {
                throw new GraphQLExecutorException(throwable);
            }
        }

        private void mergeGraphQLType(RGraphQLType rGraphQLType) throws GraphQLExecutorException {
            if (rGraphQLType instanceof RGraphQLTypeEnum) {
                waitBuildGraphQLTypeEnums.add((RGraphQLTypeEnum) rGraphQLType);
            } else if (rGraphQLType instanceof RGraphQLTypeOutObject) {
                RGraphQLTypeOutObject rGraphQLObjectType = (RGraphQLTypeOutObject) rGraphQLType;

                String rTypeGraphQLName = rGraphQLType.getName();
                Set<RGraphQLObjectTypeField> rTypeGraphQLFields = new HashSet<RGraphQLObjectTypeField>(rGraphQLObjectType.getFields());

                MergeGraphQLTypeOutObject mergeGraphQLTypeOutObject = waitBuildGraphQLTypeOutObjects.get(rTypeGraphQLName);
                if (mergeGraphQLTypeOutObject == null) {
                    mergeGraphQLTypeOutObject = new MergeGraphQLTypeOutObject(rTypeGraphQLName);
                    waitBuildGraphQLTypeOutObjects.put(rTypeGraphQLName, mergeGraphQLTypeOutObject);
                }

                //Мержим union типы
                for (String graphQLTypeUnionName : rGraphQLObjectType.getUnionGraphQLTypeNames()) {
                    MergeGraphQLTypeOutObjectUnion mergeGraphQLTypeOutObjectUnion = waitBuildGraphQLTypeOutObjectUnions.get(graphQLTypeUnionName);
                    if (mergeGraphQLTypeOutObjectUnion == null) {
                        mergeGraphQLTypeOutObjectUnion = new MergeGraphQLTypeOutObjectUnion(graphQLTypeUnionName);
                        waitBuildGraphQLTypeOutObjectUnions.put(graphQLTypeUnionName, mergeGraphQLTypeOutObjectUnion);
                    }
                    mergeGraphQLTypeOutObjectUnion.mergePossible(rGraphQLObjectType.getClassName(), rTypeGraphQLName);
                }

                //Мержим поля
                mergeGraphQLTypeOutObject.mergeFields(rTypeGraphQLFields);

            } else if (rGraphQLType instanceof RGraphQLTypeOutObjectUnion) {
                RGraphQLTypeOutObjectUnion rGraphQLTypeOutObjectUnion = (RGraphQLTypeOutObjectUnion) rGraphQLType;

                String rTypeGraphQLName = rGraphQLTypeOutObjectUnion.getName();

                MergeGraphQLTypeOutObjectUnion mergeGraphQLTypeOutObjectUnion = waitBuildGraphQLTypeOutObjectUnions.get(rTypeGraphQLName);
                if (mergeGraphQLTypeOutObjectUnion == null) {
                    mergeGraphQLTypeOutObjectUnion = new MergeGraphQLTypeOutObjectUnion(rTypeGraphQLName);
                    waitBuildGraphQLTypeOutObjectUnions.put(rTypeGraphQLName, mergeGraphQLTypeOutObjectUnion);
                }

                //Добавляем общие поля
                mergeGraphQLTypeOutObjectUnion.mergeFields(rGraphQLTypeOutObjectUnion.getFields());

            } else if (rGraphQLType instanceof RGraphQLTypeInObject) {
                RGraphQLTypeInObject rGraphQLInputObjectType = (RGraphQLTypeInObject) rGraphQLType;

                String rTypeGraphQLName = rGraphQLInputObjectType.getName();
                Set<RGraphQLInputObjectTypeField> rTypeGraphQLFields = new HashSet<>(rGraphQLInputObjectType.getFields());

                if (waitBuildGraphQLTypeInObjects.containsKey(rTypeGraphQLName)) {
                    throw new GraphQLExecutorException("Not unique name: " + rTypeGraphQLName);
                }

                waitBuildGraphQLTypeInObjects.put(rTypeGraphQLName, rTypeGraphQLFields);
            } else {
                throw new GraphQLExecutorException("Not support type: " + rGraphQLType);
            }
        }

        private GraphQLObjectType buildGraphQLTypeOutObject(Map<String, GraphQLType> graphQLTypes, MergeGraphQLTypeOutObject graphQLTypeOutObject) throws GraphQLExecutorException {
            GraphQLObjectType.Builder graphQLObjectTypeBuilder = GraphQLObjectType.newObject();
            graphQLObjectTypeBuilder.name(graphQLTypeOutObject.name);
            for (RGraphQLObjectTypeField typeGraphQLField : graphQLTypeOutObject.getFields()) {
                GraphQLFieldDefinition.Builder graphQLFieldDefinitionBuilder = GraphQLFieldDefinition.newFieldDefinition();

                graphQLFieldDefinitionBuilder.type(getGraphQLOutputType(graphQLTypes, typeGraphQLField.type))
                        .name(typeGraphQLField.externalName);

                if (typeGraphQLField.deprecated != null) {
                    graphQLFieldDefinitionBuilder.deprecate(typeGraphQLField.deprecated);
                }

                if (typeGraphQLField.isField) {
                    //Это обычное поле
                    graphQLFieldDefinitionBuilder.dataFetcher(new ExtPropertyDataFetcher(typeGraphQLField.name));
                } else {
                    //Это у нас метод
                    if (typeGraphQLField.arguments != null) {
                        for (RGraphQLObjectTypeMethodArgument argument : typeGraphQLField.arguments) {
                            GraphQLArgument.Builder argumentBuilder = GraphQLArgument.newArgument();
                            argumentBuilder.name(argument.name);

                            if (argument.isNotNull) {
                                argumentBuilder.type(new GraphQLNonNull(getGraphQLInputType(graphQLTypes, argument.type)));
                            } else {
                                argumentBuilder.type(getGraphQLInputType(graphQLTypes, argument.type));
                            }

                            graphQLFieldDefinitionBuilder.argument(argumentBuilder.build());
                        }
                    }

                    ComponentDataFetcher componentDataFetcher;
                    if (customRemoteDataFetcher != null) {
                        try {
                            componentDataFetcher = (ComponentDataFetcher) customRemoteDataFetcher.newInstance(component.getRemotes(), sdkGraphQLItemExecutor, graphQLTypeOutObject.name, typeGraphQLField);
                        } catch (ReflectiveOperationException e) {
                            throw new GraphQLExecutorException("Exception build ComponentDataFetcher", e);
                        }
                    } else {
                        componentDataFetcher = new ComponentDataFetcher(component.getRemotes(), sdkGraphQLItemExecutor, graphQLTypeOutObject.name, typeGraphQLField);
                    }
                    graphQLFieldDefinitionBuilder.dataFetcher(componentDataFetcher);
                }


                GraphQLFieldDefinition graphQLFieldDefinition = graphQLFieldDefinitionBuilder.build();

                graphQLObjectTypeBuilder.field(graphQLFieldDefinition);
            }
            GraphQLObjectType graphQLObjectType = graphQLObjectTypeBuilder.build();

            //Регистрируем этот тип
            graphQLTypes.put(graphQLTypeOutObject.name, graphQLObjectType);

            return graphQLObjectType;
        }

        private GraphQLUnionType buildGraphQLTypeOutObjectUnion(Map<String, GraphQLType> graphQLTypes, MergeGraphQLTypeOutObjectUnion mergeGraphQLTypeOutObjectUnion) {

            GraphQLUnionType.Builder builder = GraphQLUnionType.newUnionType().name(mergeGraphQLTypeOutObjectUnion.name);

            for (String possibleTypeName : mergeGraphQLTypeOutObjectUnion.getPossibleTypeNames()) {
                GraphQLObjectType possibleObject = (GraphQLObjectType) graphQLTypes.get(possibleTypeName);
                builder.possibleType(possibleObject);
            }

            builder.typeResolver(new TypeResolver() {
                @Override
                public GraphQLObjectType getType(TypeResolutionEnvironment env) {
                    String graphQLTypeName = mergeGraphQLTypeOutObjectUnion.getGraphQLTypeName(env.getObject().getClass().getName());
                    if (graphQLTypeName == null) {
                        return null;
                    } else {
                        return (GraphQLObjectType) graphQLTypes.get(graphQLTypeName);
                    }
                }
            });

            //Возможно когда-нибудь появится чтото общее между union и интерфейс


            GraphQLUnionType graphQLUnionType = builder.build();

            //Регистрируем этот тип
            graphQLTypes.put(mergeGraphQLTypeOutObjectUnion.name, graphQLUnionType);

            return graphQLUnionType;
        }

        private GraphQLEnumType buildGraphQLTypeEnum(Map<String, GraphQLType> graphQLTypes, String graphQLTypeName, Set<String> enumValues) {
            GraphQLEnumType.Builder graphQLObjectTypeEnumBuilder = GraphQLEnumType.newEnum();
            graphQLObjectTypeEnumBuilder.name(graphQLTypeName);
            for (String enumValue : enumValues) {
                graphQLObjectTypeEnumBuilder.value(enumValue);
            }

            GraphQLEnumType graphQLObjectTypeEnum = graphQLObjectTypeEnumBuilder.build();

            //Регистрируем этот тип
            graphQLTypes.put(graphQLTypeName, graphQLObjectTypeEnum);

            return graphQLObjectTypeEnum;
        }

        private GraphQLInputObjectType buildGraphQLTypeInObject(Map<String, GraphQLType> graphQLTypes, String graphQLTypeName, Set<RGraphQLInputObjectTypeField> fields) throws GraphQLExecutorException {
            GraphQLInputObjectType.Builder gBuilder = GraphQLInputObjectType.newInputObject();
            gBuilder.name(graphQLTypeName);

            for (RGraphQLInputObjectTypeField field : fields) {
                GraphQLInputObjectField.Builder fieldBuilder = GraphQLInputObjectField.newInputObjectField();
                fieldBuilder.name(field.externalName);

                if (field.isNotNull) {
                    fieldBuilder.type(new GraphQLNonNull(getGraphQLInputType(graphQLTypes, field.type)));
                } else {
                    fieldBuilder.type(getGraphQLInputType(graphQLTypes, field.type));
                }

                gBuilder.field(fieldBuilder.build());
            }

            GraphQLInputObjectType graphQLInputObjectType = gBuilder.build();

            //Регистрируем этот тип
            graphQLTypes.put(graphQLTypeName, graphQLInputObjectType);

            return graphQLInputObjectType;
        }

        private GraphQLOutputType getGraphQLOutputType(Map<String, GraphQLType> graphQLTypes, String type) throws GraphQLExecutorException {
            String[] compositeTypes = type.split(":");
            if (compositeTypes.length == 1) {//Это простой объект
                GraphQLType graphQLType = getType(graphQLTypes, type);
                if (graphQLType instanceof GraphQLOutputType) {
                    return (GraphQLOutputType) graphQLType;
                } else {
                    throw new GraphQLExecutorException("GraphQLType: " + type + " is not GraphQLOutputType");
                }
            } else if ("collection".equals(compositeTypes[0])) {
                return new GraphQLList(getType(graphQLTypes, compositeTypes[1]));
            } else {
                throw new GraphQLExecutorException("not support");
            }
        }

        private GraphQLInputType getGraphQLInputType(Map<String, GraphQLType> graphQLTypes, String type) throws GraphQLExecutorException {
            String[] compositeTypes = type.split(":");
            if (compositeTypes.length == 1) {//Это простой объект
                GraphQLType graphQLType = getType(graphQLTypes, type);
                if (graphQLType instanceof GraphQLOutputType) {
                    return (GraphQLInputType) graphQLType;
                } else if (graphQLType instanceof GraphQLInputObjectType) {
                    return (GraphQLInputType) graphQLType;
                } else {
                    throw new GraphQLExecutorException("GraphQLType: " + type + " is not GraphQLInputType");
                }
            } else if ("collection".equals(compositeTypes[0])) {
                return new GraphQLList(getGraphQLInputType(graphQLTypes, compositeTypes[1]));
            } else {
                throw new GraphQLExecutorException("not support");
            }
        }

        private GraphQLType getType(Map<String, GraphQLType> graphQLTypes, String type) {
            GraphQLType graphQLType = graphQLTypes.get(type);
            if (graphQLType != null) {
                return graphQLType;
            } else {
                return new GraphQLTypeReference(type);
            }
        }
    }
}
