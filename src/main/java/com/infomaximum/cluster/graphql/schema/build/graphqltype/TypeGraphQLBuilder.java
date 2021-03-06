package com.infomaximum.cluster.graphql.schema.build.graphqltype;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.core.remote.utils.validatorremoteobject.RemoteObjectValidator;
import com.infomaximum.cluster.core.remote.utils.validatorremoteobject.ResultValidator;
import com.infomaximum.cluster.graphql.anotation.*;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.preparecustomfield.PrepareCustomField;
import com.infomaximum.cluster.graphql.schema.GraphQLSchemaType;
import com.infomaximum.cluster.graphql.schema.scalartype.GraphQLTypeScalar;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLTypeEnum;
import com.infomaximum.cluster.graphql.schema.struct.in.RGraphQLInputObjectTypeField;
import com.infomaximum.cluster.graphql.schema.struct.in.RGraphQLTypeInObject;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeMethodArgument;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLTypeOutObjectInterface;
import com.infomaximum.cluster.graphql.struct.GOptional;
import com.infomaximum.cluster.graphql.struct.GSubscribeEvent;
import com.infomaximum.cluster.struct.Component;
import com.infomaximum.cluster.utils.ReflectionUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * Created by kris on 30.12.16.
 */
public class TypeGraphQLBuilder {

    private final static Logger log = LoggerFactory.getLogger(TypeGraphQLBuilder.class);

    private final Integer componentUniqueId;
    private final String packageName;

    private final GraphQLSchemaType graphQLSchemaType;

    private TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder;

    public TypeGraphQLBuilder(Component component, GraphQLSchemaType graphQLSchemaType) {
        this.componentUniqueId = component.getUniqueId();
        this.packageName = component.getInfo().getUuid();

        this.graphQLSchemaType = graphQLSchemaType;
    }

    public TypeGraphQLBuilder(String packageName, GraphQLSchemaType graphQLSchemaType) {
        this.componentUniqueId = null;
        this.packageName = packageName;

        this.graphQLSchemaType = graphQLSchemaType;
    }

    public TypeGraphQLBuilder withFieldConfigurationBuilder(TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder) {
        this.fieldConfigurationBuilder = fieldConfigurationBuilder;
        return this;
    }

    public Map<Class, RGraphQLType> build() throws GraphQLExecutorException {
        Reflections reflections = new Reflections(packageName);

        Map<Class, RGraphQLType> rTypeGraphQLItems = new HashMap<Class, RGraphQLType>();
        for (Class classRTypeGraphQL : reflections.getTypesAnnotatedWith(GraphQLTypeOutObject.class, true)) {
            GraphQLTypeOutObject aGraphQLType = (GraphQLTypeOutObject) classRTypeGraphQL.getAnnotation(GraphQLTypeOutObject.class);

            //?????? ????????
            String name = aGraphQLType.value();

            RGraphQLType rGraphQLType;
            if (classRTypeGraphQL.isEnum()) {
                Set<String> enumValues = null;
                enumValues = new HashSet<String>();
                for (Object oEnum : classRTypeGraphQL.getEnumConstants()) {
                    enumValues.add(((Enum) oEnum).name());
                }

                GraphQLDescription aGraphQLDescription = (GraphQLDescription) classRTypeGraphQL.getAnnotation(GraphQLDescription.class);
                String description = (aGraphQLDescription != null && !aGraphQLDescription.value().isEmpty()) ? aGraphQLDescription.value() : null;

                rGraphQLType = new RGraphQLTypeEnum(name, description, enumValues);
            } else {
                //???????????????? union ????????
                Set<String> unionGraphQLTypeNames = new HashSet<String>();
                findUnionGraphQLTypeNames(classRTypeGraphQL, unionGraphQLTypeNames);

                //???????????????? ????????
                Set<RGraphQLObjectTypeField> fields = new HashSet<RGraphQLObjectTypeField>();

                //???????????????????????? ????????
                for (Field field : classRTypeGraphQL.getDeclaredFields()) {
                    GraphQLField aGraphQLField = field.getAnnotation(GraphQLField.class);
                    if (aGraphQLField == null) continue;

                    //??????????????????, ?????? ???????????????????????? ?????? ??????????????????????
                    if (!RemoteObject.class.isAssignableFrom(field.getDeclaringClass())
                            && !Serializable.class.isAssignableFrom(field.getType())) {
                        throw new GraphQLExecutorException("Field: " + field.getName() + " in class " + classRTypeGraphQL.getName() + " return type is not serializable");
                    }

                    String typeField;
                    try {
                        typeField = getGraphQLType(field.getGenericType());
                    } catch (Exception e) {
                        throw new RuntimeException("Exception build type, class: " + classRTypeGraphQL.getName() + ", field: " + field.getName(), e);
                    }

                    String nameField = field.getName();

                    String graphQLFieldName = getGraphQLFieldName(field);
                    String graphQLFieldDeprecated = getGraphQLFieldDeprecated(field);

                    RemoteObject fieldConfiguration = null;
                    if (fieldConfigurationBuilder != null) {
                        fieldConfiguration = fieldConfigurationBuilder.build(classRTypeGraphQL, field);
                    }

                    GraphQLDescription aGraphQLDescription = field.getAnnotation(GraphQLDescription.class);
                    String description = (aGraphQLDescription != null && !aGraphQLDescription.value().isEmpty()) ? aGraphQLDescription.value() : null;

                    fields.add(new RGraphQLObjectTypeField(componentUniqueId, true, false, typeField, nameField, graphQLFieldName, fieldConfiguration, description, graphQLFieldDeprecated));
                }

                //???????????????????????? ????????????
                for (Method method : classRTypeGraphQL.getMethods()) {
                    if (method.isSynthetic()) continue;//???????????????????? ???????????????????????? ????????????

                    GraphQLField aGraphQLField = method.getAnnotation(GraphQLField.class);
                    if (aGraphQLField == null) continue;

                    fields.add(buildRGraphQLObjectTypeField(componentUniqueId, classRTypeGraphQL, method, aGraphQLField));
                }

                GraphQLDescription aTypeGraphQLDescription = (GraphQLDescription) classRTypeGraphQL.getAnnotation(GraphQLDescription.class);
                String description = (aTypeGraphQLDescription != null && !aTypeGraphQLDescription.value().isEmpty()) ? aTypeGraphQLDescription.value() : null;

                rGraphQLType = new RGraphQLTypeOutObject(name, description, classRTypeGraphQL.getName(), unionGraphQLTypeNames, fields);
            }

            rTypeGraphQLItems.put(classRTypeGraphQL, rGraphQLType);
        }

        for (Class classRTypeGraphQL : reflections.getTypesAnnotatedWith(GraphQLTypeOutObjectInterface.class, true)) {
            GraphQLTypeOutObjectInterface aGraphQLTypeOutObjectUnion = (GraphQLTypeOutObjectInterface) classRTypeGraphQL.getAnnotation(GraphQLTypeOutObjectInterface.class);

            String name = aGraphQLTypeOutObjectUnion.value();

            //???????????????? ????????
            Set<RGraphQLObjectTypeField> fields = new HashSet<RGraphQLObjectTypeField>();
            for (Method method : classRTypeGraphQL.getMethods()) {
                if (method.isSynthetic()) continue;//???????????????????? ???????????????????????? ????????????

                GraphQLField aGraphQLField = method.getAnnotation(GraphQLField.class);
                if (aGraphQLField == null) continue;

                fields.add(buildRGraphQLObjectTypeField(componentUniqueId, classRTypeGraphQL, method, aGraphQLField));
            }

            GraphQLDescription aGraphQLDescription = (GraphQLDescription) classRTypeGraphQL.getAnnotation(GraphQLDescription.class);
            String description = (aGraphQLDescription != null && !aGraphQLDescription.value().isEmpty()) ? aGraphQLDescription.value() : null;

            RGraphQLTypeOutObjectInterface rGraphQLType = new RGraphQLTypeOutObjectInterface(name, description, fields);
            rTypeGraphQLItems.put(classRTypeGraphQL, rGraphQLType);
        }

        for (Class classRTypeGraphQL : reflections.getTypesAnnotatedWith(GraphQLTypeInput.class)) {
            GraphQLTypeInput aGraphQLTypeInput = (GraphQLTypeInput) classRTypeGraphQL.getAnnotation(GraphQLTypeInput.class);

            String name = aGraphQLTypeInput.value();
            Set<RGraphQLInputObjectTypeField> fields = new HashSet<RGraphQLInputObjectTypeField>();

            //???????????????????????? ????????
            for (Field field : classRTypeGraphQL.getDeclaredFields()) {
                GraphQLTypeInput aGraphQLTypeInputField = field.getAnnotation(GraphQLTypeInput.class);
                if (aGraphQLTypeInputField == null) continue;

                String typeField;
                try {
                    typeField = getGraphQLType(field.getGenericType());
                } catch (Exception e) {
                    throw new RuntimeException("Exception build type, class: " + classRTypeGraphQL.getName() + ", field: " + field.getName(), e);
                }

                String nameField = field.getName();

                String graphQLFieldName = aGraphQLTypeInputField.value();
                if (graphQLFieldName.trim().length() == 0) {
                    graphQLFieldName = GraphQLSchemaType.convertToGraphQLName(nameField);
                }

                boolean isNotNull = (field.getAnnotation(NotNull.class) != null);

                fields.add(new RGraphQLInputObjectTypeField(typeField, nameField, graphQLFieldName, isNotNull));
            }

            GraphQLDescription aGraphQLDescription = (GraphQLDescription) classRTypeGraphQL.getAnnotation(GraphQLDescription.class);
            String description = (aGraphQLDescription != null && !aGraphQLDescription.value().isEmpty()) ? aGraphQLDescription.value() : null;

            RGraphQLTypeInObject rGraphQLType = new RGraphQLTypeInObject(name, description, fields);
            rTypeGraphQLItems.put(classRTypeGraphQL, rGraphQLType);
        }

        return rTypeGraphQLItems;
    }

    private RGraphQLObjectTypeField buildRGraphQLObjectTypeField(Integer componentUniqueId, Class classRTypeGraphQL, Method method, GraphQLField aGraphQLField) throws GraphQLExecutorException {
        //???????? ???????????????????????? ?????????? ???? ?????????????????????????? ?????????????????? RemoteObject, ???? ?????? ?????? ???????? ?????????? ???????? ???????????? ????????????????????????
        if (!RemoteObject.class.isAssignableFrom(method.getDeclaringClass())) {
            if (!Modifier.isStatic(method.getModifiers()))
                throw new GraphQLExecutorException("Method " + method.getName() + " in class " + method.getDeclaringClass().getName() + " is not static");
        }

        PrepareCustomField prepareCustomField = checkPrepareField(graphQLSchemaType, method.getReturnType());
        boolean isPrepereField = (prepareCustomField != null);

        //Prepere ???????? ???????????? ???????? ?????????????????????? static
        if (isPrepereField) {
            if (!Modifier.isStatic(method.getModifiers()))
                throw new GraphQLExecutorException("Method " + method.getName() + " in class " + method.getDeclaringClass().getName() + " is not static");
        }

        //??????????????????, ?????? ???????????????????????? ?????? ??????????????????????
        if (isPrepereField) {
            Type endType = prepareCustomField.getEndType(method.getGenericReturnType());
//			RemoteObjectValidator.validation(endType);
            Class endClazz = ReflectionUtils.getRawClass(endType);
            if (!(endClazz instanceof Serializable)) {
                throw new GraphQLExecutorException("Method: " + method.getName() + " in class " + method.getDeclaringClass().getName() + " return type is not serializable");
            }
        } else if (method.getReturnType().isAssignableFrom(GSubscribeEvent.class)) {
            //????????????????
            Type endType = method.getGenericReturnType();
            //RemoteObjectValidator.validation(endType);
            Class endClazz = ReflectionUtils.getRawClass(endType);
            if (!(endClazz instanceof Serializable)) {
                throw new GraphQLExecutorException("Method: " + method.getName() + " in class " + method.getDeclaringClass().getName() + " return type is not serializable");
            }
        } else {
            ResultValidator resultValidator = RemoteObjectValidator.validation(method.getGenericReturnType());
            if (!resultValidator.isSuccess()) {
                throw new GraphQLExecutorException("Method: " + method.getName() + " in class " + method.getDeclaringClass().getName() + " return type is not serializable. ResultValidator: " + resultValidator.toString());
            }
        }

        String typeField;
        try {
            typeField = getGraphQLType(method.getGenericReturnType());
        } catch (Exception e) {
            throw new GraphQLExecutorException("Exception build type, class: " + classRTypeGraphQL.getName() + ", method: " + method.getName(), e);
        }

        String nameMethod = method.getName();

        String graphQLFieldName = getGraphQLFieldName(method);
        String graphQLFieldDeprecated = getGraphQLFieldDeprecated(method);

        List<RGraphQLObjectTypeMethodArgument> arguments = new ArrayList<>();
        Class[] parameterTypes = method.getParameterTypes();
        Annotation[][] parametersAnnotations = method.getParameterAnnotations();
        for (int index = 0; index < parameterTypes.length; index++) {
            //???????????????? ??????????????????
            GraphQLSource aGraphQLTarget = null;
            GraphQLName aGraphQLName = null;
            GraphQLDescription aGraphQLDescription = null;
            boolean isNotNull = false;
            for (Annotation annotation : parametersAnnotations[index]) {
                if (annotation.annotationType() == GraphQLSource.class) {
                    if (!RemoteObject.class.isAssignableFrom(parameterTypes[index])) {
                        throw new RuntimeException("Class does not implement interface RemoteObject: " + parameterTypes[index]);
                    }
                    aGraphQLTarget = (GraphQLSource) annotation;
                } else if (annotation.annotationType() == GraphQLName.class) {
                    aGraphQLName = (GraphQLName) annotation;
                } else if (annotation.annotationType() == NotNull.class) {
                    isNotNull = true;
                } else if (annotation.annotationType() == GraphQLDescription.class) {
                    aGraphQLDescription = (GraphQLDescription) annotation;
                }
            }
            if (aGraphQLTarget != null) continue;//?? ?????? ???????????????????? ?????????? ???????????????????????? ???????????? ?????? ???????????????? ????????????????????
            if (aGraphQLName == null) continue;//?? ?????? ???????????????????? ?????????? ???????????????????????? ?????????????? ????????????????????

            String typeArgument;
            try {
                typeArgument = getGraphQLType(method.getGenericParameterTypes()[index]);
            } catch (Exception e) {
                throw new RuntimeException("Exception build type, class: " + classRTypeGraphQL.getName() + ", method: " + method.getName(), e);
            }
            String nameArgument = aGraphQLName.value();

            String descriptionArgument = (aGraphQLDescription != null && !aGraphQLDescription.value().isEmpty()) ? aGraphQLDescription.value() : null;

            arguments.add(new RGraphQLObjectTypeMethodArgument(typeArgument, nameArgument, nameArgument, isNotNull, descriptionArgument));
        }

        RemoteObject fieldConfiguration = null;
        if (fieldConfigurationBuilder != null) {
            fieldConfiguration = fieldConfigurationBuilder.build(classRTypeGraphQL, method);
        }

        GraphQLDescription aGraphQLDescription = method.getAnnotation(GraphQLDescription.class);
        String description = (aGraphQLDescription != null && !aGraphQLDescription.value().isEmpty()) ? aGraphQLDescription.value() : null;

        return new RGraphQLObjectTypeField(componentUniqueId, false, isPrepereField, typeField, nameMethod, graphQLFieldName, arguments, fieldConfiguration, description, graphQLFieldDeprecated);
    }

    private String getGraphQLType(Type type) throws ClassNotFoundException {
        Class rawType;
        if (type instanceof ParameterizedType) {
            rawType = (Class) ((ParameterizedType) type).getRawType();
        } else if (type instanceof Class) {
            rawType = (Class) type;
        } else {
            throw new RuntimeException("Not support type: " + type);
        }

        //?????????????????? ???? "????????????????" ?????????? ????????????
        if (rawType == Class.class) {
            Type iGenericType = ((ParameterizedType) type).getActualTypeArguments()[0];
            return getGraphQLType(iGenericType);
        }

        //?????????????????? ???? ?????????????????? ?????? ??????????????
        GraphQLTypeScalar graphQLTypeScalar = graphQLSchemaType.getTypeScalarByClass(rawType);
        if (graphQLTypeScalar != null) return graphQLTypeScalar.getName();

        //?????????????????? ???? ??????????????????
        if (rawType == ArrayList.class || rawType == HashSet.class) {
            String genericTypeName = ((ParameterizedType) type).getActualTypeArguments()[0].getTypeName();
            Class clazzGenericType = Class.forName(genericTypeName, true, Thread.currentThread().getContextClassLoader());
            return "collection:" + getGraphQLType(clazzGenericType);
        }

        //?????????????????? ???? GOptional
        if (rawType == GOptional.class) {
            Type iGenericType = ((ParameterizedType) type).getActualTypeArguments()[0];
            return getGraphQLType(iGenericType);
        }

        //?????????????????? ???? GSubscribeEvent
        if (rawType == GSubscribeEvent.class) {
            Type iGenericType = ((ParameterizedType) type).getActualTypeArguments()[0];
            return getGraphQLType(iGenericType);
        }

        //?????????????????? ???? input ????????????
        GraphQLTypeInput aGraphQLTypeInput = (GraphQLTypeInput) rawType.getAnnotation(GraphQLTypeInput.class);
        if (aGraphQLTypeInput != null) return aGraphQLTypeInput.value();

        //?????????????????? ???? out ????????????
        GraphQLTypeOutObject aGraphQLType = (GraphQLTypeOutObject) rawType.getAnnotation(GraphQLTypeOutObject.class);
        if (aGraphQLType != null) return aGraphQLType.value();

        //?????????????????? ???? union ????????????
        GraphQLTypeOutObjectInterface aGraphQLTypeOutUnion = (GraphQLTypeOutObjectInterface) rawType.getAnnotation(GraphQLTypeOutObjectInterface.class);
        if (aGraphQLTypeOutUnion != null) return aGraphQLTypeOutUnion.value();

        //?????????????????? ???????????????????????????? ?? ?????????????????? ??????????
        if (graphQLSchemaType.prepareCustomFields != null) {
            PrepareCustomField prepareCustomField = checkPrepareField(graphQLSchemaType, rawType);
            if (prepareCustomField != null) {
                return getGraphQLType(prepareCustomField.getEndType(type));
            }
        }

        throw new RuntimeException("Not support type: " + type);
    }

    /**
     * ???????????????????? ?????????????? ??????
     *
     * @param field
     * @return
     */
    private static String getGraphQLFieldName(Field field) {
        GraphQLField aGraphQLField = field.getAnnotation(GraphQLField.class);
        String graphQLName = aGraphQLField.value();
        if (graphQLName.trim().length() == 0) {
            graphQLName = GraphQLSchemaType.convertToGraphQLName(field.getName());
        }
        return graphQLName;
    }

    /**
     * ???????????????????? ?????????????? ??????
     *
     * @param method
     * @return
     */
    private static String getGraphQLFieldName(Method method) {
        GraphQLField aGraphQLField = method.getAnnotation(GraphQLField.class);
        String graphQLFieldName = aGraphQLField.value();
        if (graphQLFieldName.trim().length() == 0) {
            graphQLFieldName = method.getName();
            if (graphQLFieldName.startsWith("get") || graphQLFieldName.startsWith("set")) {
                graphQLFieldName = graphQLFieldName.substring(3);
            } else if (graphQLFieldName.startsWith("is")) {
                graphQLFieldName = graphQLFieldName.substring(2);
            }
            graphQLFieldName = GraphQLSchemaType.convertToGraphQLName(graphQLFieldName);
        }
        return graphQLFieldName;
    }

    /**
     * ???????????????????? ???????????????????? ?? Deprecated
     *
     * @param field
     * @return
     */
    private static String getGraphQLFieldDeprecated(Field field) {
        GraphQLField aGraphQLField = field.getAnnotation(GraphQLField.class);
        String deprecated = aGraphQLField.deprecated();
        if (deprecated.trim().length() == 0) {
            deprecated = null;
        }
        return deprecated;
    }

    private static String getGraphQLFieldDeprecated(Method method) {
        GraphQLField aGraphQLField = method.getAnnotation(GraphQLField.class);
        String deprecated = aGraphQLField.deprecated();
        if (deprecated.trim().length() == 0) {
            deprecated = null;
        }
        return deprecated;
    }

    private static PrepareCustomField checkPrepareField(GraphQLSchemaType graphQLSchemaType, Class clazz) {
        if (graphQLSchemaType.prepareCustomFields != null) {
            for (PrepareCustomField customField : graphQLSchemaType.prepareCustomFields) {
                if (customField.isSupport(clazz)) return customField;
            }
        }
        return null;
    }

    /**
     * ?????????????????????? ?? ???????????? ???????? ?????????????????? - union
     *
     * @param classRTypeGraphQL
     * @param unionGraphQLTypeNames
     */
    private static void findUnionGraphQLTypeNames(Class classRTypeGraphQL, Set<String> unionGraphQLTypeNames) {
        GraphQLTypeOutObjectInterface aGraphQLTypeOutObjectUnion = (GraphQLTypeOutObjectInterface) classRTypeGraphQL.getAnnotation(GraphQLTypeOutObjectInterface.class);
        if (aGraphQLTypeOutObjectUnion != null) {
            unionGraphQLTypeNames.add(aGraphQLTypeOutObjectUnion.value());
        }

        for (Class iClass : classRTypeGraphQL.getInterfaces()) {
            GraphQLTypeOutObjectInterface iAGraphQLTypeOutObjectUnion = (GraphQLTypeOutObjectInterface) iClass.getAnnotation(GraphQLTypeOutObjectInterface.class);
            if (iAGraphQLTypeOutObjectUnion == null) continue;
            unionGraphQLTypeNames.add(iAGraphQLTypeOutObjectUnion.value());
        }

        Class superClass = classRTypeGraphQL.getSuperclass();
        if (superClass == null) return;

        findUnionGraphQLTypeNames(superClass, unionGraphQLTypeNames);
    }
}
