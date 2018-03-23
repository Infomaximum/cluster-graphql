package com.infomaximum.cluster.graphql.schema;

import com.google.common.base.Defaults;
import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.anotation.GraphQLName;
import com.infomaximum.cluster.graphql.anotation.GraphQLSource;
import com.infomaximum.cluster.graphql.anotation.GraphQLTypeInput;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.fieldargument.custom.CustomFieldArgument;
import com.infomaximum.cluster.graphql.preparecustomfield.PrepareCustomField;
import com.infomaximum.cluster.graphql.schema.build.graphqltype.TypeGraphQLBuilder;
import com.infomaximum.cluster.graphql.schema.build.graphqltype.TypeGraphQLFieldConfigurationBuilder;
import com.infomaximum.cluster.graphql.schema.scalartype.GraphQLTypeScalar;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.struct.GOptional;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.cluster.struct.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class GraphQLComponentExecutor {

    private final static Logger log = LoggerFactory.getLogger(GraphQLComponentExecutor.class);

    private final Component component;

    private final Set<PrepareCustomField> prepareCustomFields;

    private final GraphQLSchemaType graphQLSchemaType;

    private ArrayList<RGraphQLType> rTypeGraphQLs;
    private Map<String, Class> classSchemas;

    public GraphQLComponentExecutor(Component component, Set<PrepareCustomField> prepareCustomFields, TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder, GraphQLSchemaType graphQLSchemaType) throws GraphQLExecutorException {
        this.component = component;
        this.graphQLSchemaType = graphQLSchemaType;
        this.prepareCustomFields = prepareCustomFields;

        TypeGraphQLBuilder typeGraphQLBuilder = new TypeGraphQLBuilder(component, graphQLSchemaType)
                .withCustomFields(prepareCustomFields)
                .withFieldConfigurationBuilder(fieldConfigurationBuilder);
        build(typeGraphQLBuilder);
    }

    public GraphQLComponentExecutor(String packageName, Set<PrepareCustomField> prepareCustomFields, TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder, GraphQLSchemaType graphQLSchemaType) throws GraphQLExecutorException {
        this.component = null;
        this.prepareCustomFields = prepareCustomFields;
        this.graphQLSchemaType = graphQLSchemaType;

        TypeGraphQLBuilder typeGraphQLBuilder = new TypeGraphQLBuilder(packageName, graphQLSchemaType)
                .withCustomFields(prepareCustomFields)
                .withFieldConfigurationBuilder(fieldConfigurationBuilder);
        build(typeGraphQLBuilder);
    }

    private void build(TypeGraphQLBuilder typeGraphQLBuilder) throws GraphQLExecutorException {
        Map<Class, RGraphQLType> rTypeGraphQLItems = typeGraphQLBuilder.build();
        rTypeGraphQLs = new ArrayList<>(typeGraphQLBuilder.build().values());

        classSchemas = new HashMap<String, Class>();
        for (Map.Entry<Class, RGraphQLType> entryTypeGraphQL: rTypeGraphQLItems.entrySet()) {
            Class classRTypeGraphQL = entryTypeGraphQL.getKey();
            RGraphQLType rGraphQLType = entryTypeGraphQL.getValue();

            if (classSchemas.containsKey(rGraphQLType.getName())) throw new RuntimeException("not unique query schema: " + rGraphQLType.getName());
            classSchemas.put(rGraphQLType.getName(), classRTypeGraphQL);
        }
    }

    public ArrayList<RGraphQLType> getGraphQLTypes() {
        return rTypeGraphQLs;
    }

    public Object execute(GRequest request, String keyFieldRequest, Object source, String graphQLTypeName, String graphQLTypeFieldName, Map<String, Serializable> arguments, boolean isPrepare) throws GraphQLExecutorDataFetcherException {
        try {
            Class classSchema = classSchemas.get(graphQLTypeName);
            if (classSchema == null) throw new RuntimeException("not support scheme: " + classSchema);

            Object object;
            if (source == null) {
                object = null;
            } else {
                if (classSchemas.get(graphQLTypeName).isAssignableFrom(source.getClass())) {
                    object = source;
                } else {
                    Constructor constructor = classSchema.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    object = constructor.newInstance();
                }
            }

            Method method = getMethod(classSchema, graphQLTypeFieldName);

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
                    argumentValue = getValue(method.getGenericParameterTypes()[index], arguments.get(argumentName), isPresent);
                } else {
                    //возможно особый аргумент
                    Class classType = methodParameterTypes[index];
                    if (GRequest.class.isAssignableFrom(classType)) {
                        argumentValue = request;
                    } else {
                        boolean isSuccessFindEnvironment = false;
                        if (graphQLSchemaType != null) {
                            for (CustomFieldArgument customArgument : graphQLSchemaType.customArguments) {
                                if (customArgument.isSupport(classType)) {
                                    argumentValue = customArgument.getValue(request, classType);
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


            Object result;
            try {
                result = method.invoke(object, methodParameters);
            } catch (InvocationTargetException te) {
                throw new GraphQLExecutorDataFetcherException(te.getTargetException());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

            if (result!=null && prepareCustomFields != null) {
                for (PrepareCustomField prepareCustomField: prepareCustomFields) {
                    if (prepareCustomField.isSupport(result.getClass())) {
                        if (isPrepare) {
                            result = prepareCustomField.prepare(keyFieldRequest, result);
                        } else {
                            RemoteObject remoteObjectSource = null;
                            if (source instanceof RemoteObject) {
                                remoteObjectSource = (RemoteObject) source;
                            }
                            result = prepareCustomField.execute(request, keyFieldRequest, remoteObjectSource);
                        }
                        break;
                    }
                }
            }

            return result;
        } catch (ReflectiveOperationException re) {
            throw new RuntimeException(re);
        }
    }

    private Object getValue(Type type, Object inputValue, boolean isPresent) throws ReflectiveOperationException {
        Class clazz;
        if (type instanceof ParameterizedType) {
            clazz = (Class) ((ParameterizedType) type).getRawType();
        } else {
            clazz = (Class) type;
        }

        if (inputValue==null) {
            if (clazz.isPrimitive()) return Defaults.defaultValue(clazz);
            if (clazz == GOptional.class) return new GOptional(null, isPresent);
            return null;
        }

        //Проверяем на скалярный тип объекта
        GraphQLTypeScalar graphQLTypeScalar = graphQLSchemaType.getTypeScalarByClass(clazz);
        if (graphQLTypeScalar != null)
            return graphQLTypeScalar.getGraphQLScalarType().getCoercing().parseValue(inputValue);


        if (clazz.isEnum()) {
            return Enum.valueOf(clazz, (String) inputValue);
        } else if (clazz == GOptional.class) {
            return new GOptional(getValue(((ParameterizedType) type).getActualTypeArguments()[0], inputValue, true), isPresent);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection collection;
            if (clazz.isAssignableFrom(ArrayList.class)) {
                collection = new ArrayList();
            } else if (clazz.isAssignableFrom(HashSet.class)) {
                collection = new HashSet();
            } else {
                throw new RuntimeException("Not support type collection: " + clazz);
            }
            for (Object iObject : (Collection) inputValue) {
                collection.add(getValue(((ParameterizedType) type).getActualTypeArguments()[0], iObject, true));
            }
            return collection;
        } else if (clazz.getAnnotation(GraphQLTypeInput.class) != null) {
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

                field.set(value, getValue(field.getGenericType(), fieldValues.get(externalName), fieldValues.containsKey(externalName)));
            }

            return value;
        } else {
            throw new GraphQLExecutorException("Not support type: " + type);
        }
    }

    //TODO Ulitin V. Если когда нибудь у нас появится перегрузка методов, переписать
    private static Method getMethod(Class classSchema, String methodName) {
        Method findMethod=null;
        for (Method method: classSchema.getMethods()) {
            if (method.isSynthetic()) continue; //Игнорируем генерируемые методы
            if (method.getName().equals(methodName)) {
                if (findMethod==null) {
                    findMethod=method;
                } else {
                    throw new RuntimeException("not support overload method: " + methodName + " in class: " + classSchema);
                }
            }
        }

        if (findMethod==null) throw new RuntimeException("not found method: " + methodName + " in " + classSchema);
        return findMethod;
    }
}
