package com.infomaximum.cluster.graphql.executor.component;

import com.google.common.base.Defaults;
import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.anotation.GraphQLName;
import com.infomaximum.cluster.graphql.anotation.GraphQLSource;
import com.infomaximum.cluster.graphql.anotation.GraphQLTypeInput;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorInvalidSyntaxException;
import com.infomaximum.cluster.graphql.fieldargument.custom.CustomFieldArgument;
import com.infomaximum.cluster.graphql.preparecustomfield.PrepareCustomField;
import com.infomaximum.cluster.graphql.schema.GraphQLSchemaType;
import com.infomaximum.cluster.graphql.schema.build.graphqltype.TypeGraphQLBuilder;
import com.infomaximum.cluster.graphql.schema.build.graphqltype.TypeGraphQLFieldConfigurationBuilder;
import com.infomaximum.cluster.graphql.schema.scalartype.GraphQLTypeScalar;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.struct.ContextRequest;
import com.infomaximum.cluster.graphql.struct.GOptional;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.cluster.graphql.struct.GSubscribeEvent;
import com.infomaximum.cluster.struct.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class GraphQLComponentExecutor {

    private final static Logger log = LoggerFactory.getLogger(GraphQLComponentExecutor.class);

    private final GraphQLSchemaType graphQLSchemaType;

    private ArrayList<RGraphQLType> rTypeGraphQLs;
    private Map<String, Class> classSchemas;

    public GraphQLComponentExecutor(Component component, TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder, GraphQLSchemaType graphQLSchemaType) throws GraphQLExecutorException {
        this.graphQLSchemaType = graphQLSchemaType;

        TypeGraphQLBuilder typeGraphQLBuilder = new TypeGraphQLBuilder(component, graphQLSchemaType)
                .withFieldConfigurationBuilder(fieldConfigurationBuilder);
        build(typeGraphQLBuilder);
    }

    public GraphQLComponentExecutor(String packageName, TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder, GraphQLSchemaType graphQLSchemaType) throws GraphQLExecutorException {
        this.graphQLSchemaType = graphQLSchemaType;

        TypeGraphQLBuilder typeGraphQLBuilder = new TypeGraphQLBuilder(packageName, graphQLSchemaType)
                .withFieldConfigurationBuilder(fieldConfigurationBuilder);
        build(typeGraphQLBuilder);
    }

    private void build(TypeGraphQLBuilder typeGraphQLBuilder) throws GraphQLExecutorException {
        Map<Class, RGraphQLType> rTypeGraphQLItems = typeGraphQLBuilder.build();
        rTypeGraphQLs = new ArrayList<>(typeGraphQLBuilder.build().values());

        classSchemas = new HashMap<String, Class>();
        for (Map.Entry<Class, RGraphQLType> entryTypeGraphQL : rTypeGraphQLItems.entrySet()) {
            Class classRTypeGraphQL = entryTypeGraphQL.getKey();
            RGraphQLType rGraphQLType = entryTypeGraphQL.getValue();

            if (classSchemas.containsKey(rGraphQLType.getName()))
                throw new RuntimeException("not unique query schema: " + rGraphQLType.getName());
            classSchemas.put(rGraphQLType.getName(), classRTypeGraphQL);
        }
    }

    public ArrayList<RGraphQLType> getGraphQLTypes() {
        return rTypeGraphQLs;
    }

    public Serializable prepare(Component component, String keyField, String graphQLTypeName, String graphQLTypeFieldName, Map<String, Serializable> arguments, ContextRequest context) throws GraphQLExecutorDataFetcherException {
        Object prepareResultObject = executeGraphQLMethod(null, graphQLTypeName, graphQLTypeFieldName, arguments, context);
        if (prepareResultObject == null) {
            throw new GraphQLExecutorException("Class: " + classSchemas.get(graphQLTypeName) + ", method: " + graphQLTypeFieldName + " returning is null prepare object");
        }

        for (PrepareCustomField prepareCustomField : graphQLSchemaType.prepareCustomFields) {
            if (prepareCustomField.isSupport(prepareResultObject.getClass())) {
                return prepareCustomField.requestPrepare(component, keyField, prepareResultObject, context);
            }
        }
        throw new GraphQLExecutorException("Not found prepare handler for: " + prepareResultObject);
    }

    public Serializable executePrepare(String keyField, RemoteObject source, ContextRequest context) {
        if (graphQLSchemaType.prepareCustomFields.size() != 1)
            throw new RuntimeException("Not implemented support many prepareCustomFields");

        PrepareCustomField prepareCustomField = graphQLSchemaType.prepareCustomFields.iterator().next();
        return prepareCustomField.execute(keyField, source, context);
    }

    public Serializable execute(RemoteObject source, String graphQLTypeName, String graphQLTypeFieldName, Map<String, Serializable> arguments, ContextRequest context) throws GraphQLExecutorDataFetcherException {
        Object result = executeGraphQLMethod(source, graphQLTypeName, graphQLTypeFieldName, arguments, context);
        if (result instanceof GSubscribeEvent) {
            return ((GSubscribeEvent) result).getSubscribeValue();
        } else {
            return (Serializable) result;
        }
    }

    private Object executeGraphQLMethod(Object source, String graphQLTypeName, String graphQLTypeFieldName, Map<String, Serializable> arguments, ContextRequest context) throws GraphQLExecutorDataFetcherException {
        try {
            Method method = getMethod(graphQLTypeName, graphQLTypeFieldName);

            Class classSchema = classSchemas.get(graphQLTypeName);

            Object object = null;
            if (source == null || classSchema.isAssignableFrom(source.getClass())) {
                object = source;
            }

            Class[] methodParameterTypes = method.getParameterTypes();
            Annotation[][] parametersAnnotations = method.getParameterAnnotations();
            Object[] methodParameters = new Object[methodParameterTypes.length];
            for (int index = 0; index < methodParameters.length; index++) {
                //Собираем аннотации
                GraphQLSource aGraphQLTarget = null;
                GraphQLName graphQLAnnotation = null;
                for (Annotation annotation : parametersAnnotations[index]) {
                    if (annotation.annotationType() == GraphQLSource.class) {
                        aGraphQLTarget = (GraphQLSource) annotation;
                    } else if (annotation.annotationType() == GraphQLName.class) {
                        graphQLAnnotation = (GraphQLName) annotation;
                    }
                }

                Object argumentValue = null;
                if (aGraphQLTarget != null) {
                    argumentValue = source;
                } else if (graphQLAnnotation != null) {
                    String argumentName = graphQLAnnotation.value();
                    boolean isPresent = arguments.containsKey(argumentName);
                    argumentValue = getInputValue(method.getGenericParameterTypes()[index], arguments.get(argumentName), isPresent);
                } else {
                    //возможно особый аргумент
                    Class classType = methodParameterTypes[index];
                    if (GRequest.class.isAssignableFrom(classType)) {
                        argumentValue = context.getRequest();
                    } else {
                        boolean isSuccessFindEnvironment = false;
                        if (graphQLSchemaType != null) {
                            for (CustomFieldArgument customArgument : graphQLSchemaType.customArguments) {
                                if (customArgument.isSupport(classType)) {
                                    argumentValue = customArgument.getValue(classType, method, context);
                                    isSuccessFindEnvironment = true;
                                }
                            }
                        }
                        if (!isSuccessFindEnvironment) {
                            throw new RuntimeException("Nothing argument type: " + classType + ", index: " + index + ", method: " + method + ", class: " + classSchema);
                        }
                    }
                }
                methodParameters[index] = argumentValue;
            }

            try {
                return method.invoke(object, methodParameters);
            } catch (InvocationTargetException te) {
                throw new GraphQLExecutorDataFetcherException(te.getTargetException());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } catch (ReflectiveOperationException re) {
            throw new RuntimeException(re);
        }
    }

    private Object getInputValue(Type type, Object inputValue, boolean isPresent) throws ReflectiveOperationException {
        Class clazz;
        if (type instanceof ParameterizedType) {
            clazz = (Class) ((ParameterizedType) type).getRawType();
        } else {
            clazz = (Class) type;
        }

        if (inputValue == null) {
            if (clazz.isPrimitive()) return Defaults.defaultValue(clazz);
            if (clazz == GOptional.class) return new GOptional(null, isPresent);
            return null;
        }

        //Проверяем на скалярный тип объекта
        GraphQLTypeScalar graphQLTypeScalar = graphQLSchemaType.getTypeScalarByClass(clazz);
        if (graphQLTypeScalar != null) {
            return graphQLTypeScalar.getGraphQLScalarType().getCoercing().parseValue(inputValue);
        }

        if (clazz.isEnum()) {
            try {
                return Enum.valueOf(clazz, (String) inputValue);
            } catch (Exception e) {
                throw new GraphQLExecutorInvalidSyntaxException(e);
            }
        } else if (clazz == GOptional.class) {
            return new GOptional(getInputValue(((ParameterizedType) type).getActualTypeArguments()[0], inputValue, true), isPresent);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            if (!Collection.class.isAssignableFrom(inputValue.getClass())) {
                throw new GraphQLExecutorInvalidSyntaxException();
            }

            if (clazz.isAssignableFrom(ArrayList.class)) {
                List list = new ArrayList();
                for (Object iObject : (Collection) inputValue) {
                    Object element = getInputValue(((ParameterizedType) type).getActualTypeArguments()[0], iObject, true);
                    list.add(element);
                }
                return list;
            } else if (clazz.isAssignableFrom(HashSet.class)) {
                Set set = new HashSet();
                for (Object iObject : (Collection) inputValue) {
                    Object element = getInputValue(((ParameterizedType) type).getActualTypeArguments()[0], iObject, true);
                    if (element == null) continue;
                    set.add(element);
                }
                return set;
            } else {
                throw new RuntimeException("Not support type collection: " + clazz);
            }
        } else if (clazz.getAnnotation(GraphQLTypeInput.class) != null) {
            if (!Map.class.isAssignableFrom(inputValue.getClass())) {
                throw new GraphQLExecutorInvalidSyntaxException();
            }

            Map<String, Object> fieldValues = (Map<String, Object>) inputValue;

            Constructor constructor = clazz.getConstructor();
            Object value = constructor.newInstance();
            for (Field field : clazz.getDeclaredFields()) {
                GraphQLTypeInput aGraphQLTypeInput = field.getAnnotation(GraphQLTypeInput.class);
                if (aGraphQLTypeInput == null) continue;

                //Игнорируем права доступа
                field.setAccessible(true);

                String externalName = aGraphQLTypeInput.value();
                if (externalName == null || externalName.isEmpty()) externalName = field.getName();

                field.set(value, getInputValue(field.getGenericType(), fieldValues.get(externalName), fieldValues.containsKey(externalName)));
            }

            return value;
        } else {
            throw new GraphQLExecutorException("Not support type: " + type);
        }
    }

    //TODO Ulitin V. Если когда нибудь у нас появится перегрузка методов, переписать
    private Method getMethod(String graphQLTypeName, String graphQLTypeFieldName) {
        Class classSchema = classSchemas.get(graphQLTypeName);
        if (classSchema == null) throw new RuntimeException("not support scheme: " + classSchema);

        Method findMethod = null;
        for (Method method : classSchema.getMethods()) {
            if (method.isSynthetic()) continue; //Игнорируем генерируемые методы
            if (method.getName().equals(graphQLTypeFieldName)) {
                if (findMethod == null) {
                    findMethod = method;
                } else {
                    throw new RuntimeException("not support overload method: " + graphQLTypeFieldName + " in class: " + classSchema);
                }
            }
        }

        if (findMethod == null)
            throw new RuntimeException("not found method: " + graphQLTypeFieldName + " in " + classSchema);
        return findMethod;
    }
}
