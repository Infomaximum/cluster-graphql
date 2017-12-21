package com.infomaximum.cluster.graphql.remote.graphql;

import com.google.common.base.Defaults;
import com.infomaximum.cluster.core.remote.AbstractRController;
import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.anotation.GraphQLName;
import com.infomaximum.cluster.graphql.anotation.GraphQLSource;
import com.infomaximum.cluster.graphql.anotation.GraphQLTypeInput;
import com.infomaximum.cluster.graphql.customtype.CustomEnvType;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.schema.utils.BuildTypeGraphQLUtils;
import com.infomaximum.cluster.graphql.struct.GOptional;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.cluster.graphql.struct.GRequestItem;
import com.infomaximum.cluster.struct.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;


/**
 * Created by kris on 19.11.16.
 */
public class RControllerGraphQLImpl<T extends Component> extends AbstractRController<T> implements RControllerGraphQL {

    private final static Logger log = LoggerFactory.getLogger(RControllerGraphQLImpl.class);

    private final Set<CustomEnvType> customEnvTypes;

    private final List<RGraphQLType> rTypeGraphQLs;
    private final Map<String, Class> classSchemas;

    public RControllerGraphQLImpl(T component) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        this(component, null);
    }

    public RControllerGraphQLImpl(T component, Set<CustomEnvType> customEnvTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        super(component);
        this.customEnvTypes = customEnvTypes;

        Map<Class, RGraphQLType> rTypeGraphQLItems = BuildTypeGraphQLUtils.findTypeGraphQL(component.getInfo().getUuid());
        rTypeGraphQLs = Collections.unmodifiableList(new ArrayList<>(rTypeGraphQLItems.values()));

        classSchemas = new HashMap<String, Class>();
        for (Map.Entry<Class, RGraphQLType> entryTypeGraphQL: rTypeGraphQLItems.entrySet()) {
            Class classRTypeGraphQL = entryTypeGraphQL.getKey();
            RGraphQLType rGraphQLType = entryTypeGraphQL.getValue();

            if (classSchemas.containsKey(rGraphQLType.getName())) throw new RuntimeException("not unique query schema: " + rGraphQLType.getName());
            classSchemas.put(rGraphQLType.getName(), classRTypeGraphQL);
        }
    }

    @Override
    public List<RGraphQLType> getCustomTypes() {
        return rTypeGraphQLs;
    }

    @Override
    public Object execute(GRequest request, GRequestItem gRequestItem, String graphQLTypeName, String graphQLTypeMethodName, Map<String, Object> arguments) throws GraphQLExecutorDataFetcherException {
        try {
            Class classSchema = classSchemas.get(graphQLTypeName);
            if (classSchema == null) throw new RuntimeException("not support scheme: " + classSchema);

            Object object;
            if (RemoteObject.instanceOf(classSchema)
                    &&
                    classSchemas.get(graphQLTypeName).isAssignableFrom(gRequestItem.source.getClass())) {
                object = gRequestItem.source;
            } else {
                Constructor constructor = classSchema.getConstructor();
                object = constructor.newInstance();
            }


            Method method = getMethod(classSchema, graphQLTypeMethodName);

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
                    argumentValue = gRequestItem.source;
                } else if (graphQLAnnotation != null) {
                    String argumentName = graphQLAnnotation.value();
                    boolean isPresent = gRequestItem.receivedArguments.contains(argumentName);
                    argumentValue = getValue(method.getGenericParameterTypes()[index], arguments.get(argumentName), isPresent);
                } else {
                    //возможно особый аргумент
                    Class classType = methodParameterTypes[index];
                    if (classType == GRequest.class) {
                        argumentValue = request;
                    } else {
                        boolean isSuccessFindEnvironment = false;
                        if (customEnvTypes !=null) {
                            for (CustomEnvType customEnvironment: customEnvTypes) {
                                if (customEnvironment.isSupport(classType)) {
                                    argumentValue = customEnvironment.getValue(request, classType);
                                    isSuccessFindEnvironment = true;
                                }
                            }
                        }
                        if (!isSuccessFindEnvironment) {
                            throw new RuntimeException("Nothing argument index: " + index);
                        }
                    }
                }
                methodParameters[index] = argumentValue;
            }


            Object result;
            try {
                result = method.invoke(object, methodParameters);
            } catch (InvocationTargetException te) {
                Throwable cause = te.getTargetException();
                throw new GraphQLExecutorDataFetcherException(cause);
            } catch (Throwable e) {
                log.error("Ошибка вызова метода: {}, у объекта: {}", method.getName(), object.getClass().getName(), e);
                throw new RuntimeException(e);
            }
            return result;
        } catch (ReflectiveOperationException re) {
            throw new RuntimeException(re);
        }
    }

    private static Object getValue(Type type, Object inputValue, boolean isPresent) throws ReflectiveOperationException {
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


        if (clazz.isEnum()) {
            return Enum.valueOf(clazz, (String) inputValue);
        } else if (clazz == GOptional.class) {
            return new GOptional(getValue(((ParameterizedType) type).getActualTypeArguments()[0], inputValue, true), isPresent);
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Collection collection;
            if (clazz.isAssignableFrom(List.class)) {
                collection = new ArrayList();
            } else if (clazz.isAssignableFrom(Set.class)) {
                collection = new HashSet();
            } else {
                throw new RuntimeException("Not suuport type collection: " + clazz);
            }
            for (Object iObject : (Collection) inputValue) {
                collection.add(getValue(((ParameterizedType) type).getActualTypeArguments()[0], iObject, true));
            }
            return collection;
        } else if (clazz.getAnnotation(GraphQLTypeInput.class) != null) {
            if (inputValue == null) return null;

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
        } else if (clazz == Double.class || clazz == double.class) {
            return ((Number)inputValue).doubleValue();
        } else {
            return inputValue;
        }
    }

    //TODO Ulitin V. Если когда нибудь у нас появится перегрузка методов, переписать
    private static Method getMethod(Class classSchema, String methodName) {
        Method findMethod=null;
        for (Method method: classSchema.getDeclaredMethods()) {
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
