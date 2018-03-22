package com.infomaximum.cluster.graphql.executor.builder;

import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.executor.GraphQLExecutor;
import com.infomaximum.cluster.graphql.executor.GraphQLExecutorImpl;
import com.infomaximum.cluster.graphql.executor.GraphQLExecutorPrepareImpl;
import com.infomaximum.cluster.graphql.fieldargument.FieldArgumentConverter;
import com.infomaximum.cluster.graphql.preparecustomfield.PrepareCustomField;
import com.infomaximum.cluster.graphql.remote.graphql.RControllerGraphQL;
import com.infomaximum.cluster.graphql.schema.GraphQLComponentExecutor;
import com.infomaximum.cluster.graphql.schema.TypeSchema;
import com.infomaximum.cluster.graphql.schema.build.MergeGraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.schema.build.MergeGraphQLTypeOutObjectUnion;
import com.infomaximum.cluster.graphql.schema.build.graphqltype.TypeGraphQLFieldConfigurationBuilder;
import com.infomaximum.cluster.graphql.schema.datafetcher.ComponentDataFetcher;
import com.infomaximum.cluster.graphql.schema.datafetcher.ExtPropertyDataFetcher;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLTypeEnum;
import com.infomaximum.cluster.graphql.schema.struct.in.RGraphQLInputObjectTypeField;
import com.infomaximum.cluster.graphql.schema.struct.in.RGraphQLTypeInObject;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeMethodArgument;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.schema.struct.out.union.RGraphQLTypeOutObjectUnion;
import com.infomaximum.cluster.struct.Component;
import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.schema.*;

import java.lang.reflect.Constructor;
import java.util.*;

import static graphql.schema.GraphQLSchema.newSchema;

public class GraphQLExecutorBuilder {

    private final Component component;
    private final String sdkPackagePath;
    private final Constructor customRemoteDataFetcher;

    private final Set<PrepareCustomField> prepareCustomFields;
    private final TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder;

    private GraphQLComponentExecutor sdkGraphQLItemExecutor;

    private final FieldArgumentConverter fieldArgumentConverter;

    public GraphQLExecutorBuilder(
            Component component,
            String sdkPackagePath,
            Constructor customRemoteDataFetcher,
            Set<PrepareCustomField> prepareCustomFields,
            TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder,
            FieldArgumentConverter fieldArgumentConverter
    ) {

        this.component = component;
        this.sdkPackagePath = sdkPackagePath;
        this.customRemoteDataFetcher = customRemoteDataFetcher;
        this.prepareCustomFields = prepareCustomFields;
        this.fieldConfigurationBuilder = fieldConfigurationBuilder;
        this.fieldArgumentConverter = fieldArgumentConverter;
    }

    public GraphQLExecutor build() throws GraphQLExecutorException {
        try {

            //Собираем какие типы у нас вообще есть
            List<RGraphQLTypeEnum> buildGraphQLTypeEnums = new ArrayList<RGraphQLTypeEnum>();
            Map<String, MergeGraphQLTypeOutObject> buildGraphQLTypeOutObjects = new HashMap<String, MergeGraphQLTypeOutObject>();
            Map<String, MergeGraphQLTypeOutObjectUnion> buildGraphQLTypeOutObjectUnions = new HashMap<String, MergeGraphQLTypeOutObjectUnion>();
            Map<String, Set<RGraphQLInputObjectTypeField>> buildGraphQLTypeInObjects = new HashMap<String, Set<RGraphQLInputObjectTypeField>>();

            //Собираем встроенные
            if (sdkPackagePath!=null) {
                sdkGraphQLItemExecutor = new GraphQLComponentExecutor(
                        sdkPackagePath, prepareCustomFields, fieldConfigurationBuilder, fieldArgumentConverter
                );
                for (RGraphQLType rGraphQLType : sdkGraphQLItemExecutor.getGraphQLTypes()) {
                    mergeGraphQLType(
                            buildGraphQLTypeEnums,
                            buildGraphQLTypeOutObjects,
                            buildGraphQLTypeOutObjectUnions,
                            buildGraphQLTypeInObjects,
                            rGraphQLType
                    );
                }
            }

            //Запрашиваем у подсистем
            Collection<RControllerGraphQL> rControllerGraphQLs = component.getRemotes().getControllers(RControllerGraphQL.class);
            for (RControllerGraphQL rControllerGraphQL : rControllerGraphQLs) {
                for (RGraphQLType rGraphQLType : rControllerGraphQL.getGraphQLTypes()) {
                    mergeGraphQLType(
                            buildGraphQLTypeEnums,
                            buildGraphQLTypeOutObjects,
                            buildGraphQLTypeOutObjectUnions,
                            buildGraphQLTypeInObjects,
                            rGraphQLType
                    );
                }
            }

            //В этот map добавляются все построенные типы
            Map<String, GraphQLType> graphQLTypes = new HashMap<String, GraphQLType>();

            //Добавляем все скаляры
            for (GraphQLScalarType graphQLScalarType : fieldArgumentConverter.scalarTypes) {
                String name = graphQLScalarType.getName().toLowerCase();
                graphQLTypes.put(name, graphQLScalarType);
                graphQLTypes.put("collection:" + name, new GraphQLList(graphQLScalarType));
            }

            //Добавляем все enum
            for (RGraphQLTypeEnum rGraphQLEnumType: buildGraphQLTypeEnums) {
                buildGraphQLTypeEnum(graphQLTypes, rGraphQLEnumType.getName(), rGraphQLEnumType.getEnumValues());
            }

            //Разбираемся с зависимостями input объектами
            for (Map.Entry<String, Set<RGraphQLInputObjectTypeField>> entry: buildGraphQLTypeInObjects.entrySet()) {
                String graphQLTypeName = entry.getKey();
                Set<RGraphQLInputObjectTypeField> graphQLTypeFields = entry.getValue();

                buildGraphQLTypeInObject(graphQLTypes, graphQLTypeName, graphQLTypeFields);
            }


            //Разбираемся с зависимостями output объектов
            Map<String, MergeGraphQLTypeOutObject> waitBuildGraphQLTypeOutObjects = new HashMap<String, MergeGraphQLTypeOutObject>(buildGraphQLTypeOutObjects);
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
            for (MergeGraphQLTypeOutObjectUnion mergeGraphQLTypeOutObjectUnion : buildGraphQLTypeOutObjectUnions.values()) {
                buildGraphQLTypeOutObjectUnion(graphQLTypes, mergeGraphQLTypeOutObjectUnion);
            }


            GraphQLSchema schema = newSchema()
                    .query((GraphQLObjectType) graphQLTypes.get(TypeSchema.QUERY.getValue()))
                    .mutation((GraphQLObjectType) graphQLTypes.get(TypeSchema.MUTATION.getValue()))
                    .build(new HashSet<GraphQLType>(graphQLTypes.values()));

            GraphQL graphQL = GraphQL.newGraphQL(schema).build();

            if (prepareCustomFields==null || prepareCustomFields.isEmpty()) {
                return new GraphQLExecutorImpl(schema, graphQL);
            } else {
                return new GraphQLExecutorPrepareImpl(component, schema, graphQL, buildGraphQLTypeOutObjects);
            }
        } catch (Throwable throwable) {
            throw new GraphQLExecutorException(throwable);
        }
    }

    private void mergeGraphQLType(
            List<RGraphQLTypeEnum> buildGraphQLTypeEnums,
            Map<String, MergeGraphQLTypeOutObject> buildGraphQLTypeOutObjects,
            Map<String, MergeGraphQLTypeOutObjectUnion> buildGraphQLTypeOutObjectUnions,
            Map<String, Set<RGraphQLInputObjectTypeField>> buildGraphQLTypeInObjects,

            RGraphQLType rGraphQLType) throws GraphQLExecutorException {
        if (rGraphQLType instanceof RGraphQLTypeEnum) {
            buildGraphQLTypeEnums.add((RGraphQLTypeEnum) rGraphQLType);
        } else if (rGraphQLType instanceof RGraphQLTypeOutObject) {
            RGraphQLTypeOutObject rGraphQLObjectType = (RGraphQLTypeOutObject) rGraphQLType;

            String rTypeGraphQLName = rGraphQLType.getName();
            Set<RGraphQLObjectTypeField> rTypeGraphQLFields = new HashSet<RGraphQLObjectTypeField>(rGraphQLObjectType.getFields());

            MergeGraphQLTypeOutObject mergeGraphQLTypeOutObject = buildGraphQLTypeOutObjects.get(rTypeGraphQLName);
            if (mergeGraphQLTypeOutObject == null) {
                mergeGraphQLTypeOutObject = new MergeGraphQLTypeOutObject(rTypeGraphQLName);
                buildGraphQLTypeOutObjects.put(rTypeGraphQLName, mergeGraphQLTypeOutObject);
            }

            //Мержим union типы
            for (String graphQLTypeUnionName : rGraphQLObjectType.getUnionGraphQLTypeNames()) {
                MergeGraphQLTypeOutObjectUnion mergeGraphQLTypeOutObjectUnion = buildGraphQLTypeOutObjectUnions.get(graphQLTypeUnionName);
                if (mergeGraphQLTypeOutObjectUnion == null) {
                    mergeGraphQLTypeOutObjectUnion = new MergeGraphQLTypeOutObjectUnion(graphQLTypeUnionName);
                    buildGraphQLTypeOutObjectUnions.put(graphQLTypeUnionName, mergeGraphQLTypeOutObjectUnion);
                }
                mergeGraphQLTypeOutObjectUnion.mergePossible(rGraphQLObjectType.getClassName(), rTypeGraphQLName);
            }

            //Мержим поля
            mergeGraphQLTypeOutObject.mergeFields(rTypeGraphQLFields);

        } else if (rGraphQLType instanceof RGraphQLTypeOutObjectUnion) {
            RGraphQLTypeOutObjectUnion rGraphQLTypeOutObjectUnion = (RGraphQLTypeOutObjectUnion) rGraphQLType;

            String rTypeGraphQLName = rGraphQLTypeOutObjectUnion.getName();

            MergeGraphQLTypeOutObjectUnion mergeGraphQLTypeOutObjectUnion = buildGraphQLTypeOutObjectUnions.get(rTypeGraphQLName);
            if (mergeGraphQLTypeOutObjectUnion == null) {
                mergeGraphQLTypeOutObjectUnion = new MergeGraphQLTypeOutObjectUnion(rTypeGraphQLName);
                buildGraphQLTypeOutObjectUnions.put(rTypeGraphQLName, mergeGraphQLTypeOutObjectUnion);
            }

            //Добавляем общие поля
            mergeGraphQLTypeOutObjectUnion.mergeFields(rGraphQLTypeOutObjectUnion.getFields());

        } else if (rGraphQLType instanceof RGraphQLTypeInObject) {
            RGraphQLTypeInObject rGraphQLInputObjectType = (RGraphQLTypeInObject) rGraphQLType;

            String rTypeGraphQLName = rGraphQLInputObjectType.getName();
            Set<RGraphQLInputObjectTypeField> rTypeGraphQLFields = new HashSet<>(rGraphQLInputObjectType.getFields());

            if (buildGraphQLTypeInObjects.containsKey(rTypeGraphQLName)) {
                throw new GraphQLExecutorException("Not unique name: " + rTypeGraphQLName);
            }

            buildGraphQLTypeInObjects.put(rTypeGraphQLName, rTypeGraphQLFields);
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
