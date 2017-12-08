package com.infomaximum.cluster.graphql.schema.utils;

import com.google.common.base.CaseFormat;
import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.anotation.*;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.schema.struct.RGraphQLTypeEnum;
import com.infomaximum.cluster.graphql.schema.struct.input.RGraphQLInputObjectTypeField;
import com.infomaximum.cluster.graphql.schema.struct.input.RGraphQLTypeInObject;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLTypeOutObjectUnion;
import com.infomaximum.cluster.graphql.schema.struct.output.RGraphQLObjectTypeField;
import com.infomaximum.cluster.graphql.schema.struct.output.RGraphQLObjectTypeMethodArgument;
import com.infomaximum.cluster.graphql.schema.struct.output.RGraphQLTypeOutObject;
import com.infomaximum.cluster.graphql.struct.GOptional;
import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.cluster.struct.Component;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Created by kris on 30.12.16.
 */
public class BuildTypeGraphQLUtils {

    private final static Logger log = LoggerFactory.getLogger(BuildTypeGraphQLUtils.class);

    public static Map<Class, RGraphQLType> findTypeGraphQL(String subsystem) throws ClassNotFoundException {
        Reflections reflections = new Reflections(subsystem);

        Map<Class, RGraphQLType> rTypeGraphQLItems = new HashMap<Class, RGraphQLType>();
        for (Class classRTypeGraphQL : reflections.getTypesAnnotatedWith(GraphQLTypeOutObject.class, true)) {
            GraphQLTypeOutObject aGraphQLType = (GraphQLTypeOutObject) classRTypeGraphQL.getAnnotation(GraphQLTypeOutObject.class);

            //Имя типа
            String name = aGraphQLType.value();

            RGraphQLType rGraphQLType;
            if (classRTypeGraphQL.isEnum()) {
                Set<String> enumValues = null;
                enumValues = new HashSet<String>();
                for (Object oEnum: classRTypeGraphQL.getEnumConstants()){
                    enumValues.add(((Enum)oEnum).name());
                }

                rGraphQLType = new RGraphQLTypeEnum(name, enumValues);
            } else {
                //Собираем union типы
                Set<String> unionGraphQLTypeNames = new HashSet<String>();
                for (Class iClass : classRTypeGraphQL.getInterfaces()) {
                    GraphQLTypeOutObjectUnion aGraphQLTypeOutObjectUnion = (GraphQLTypeOutObjectUnion) iClass.getAnnotation(GraphQLTypeOutObjectUnion.class);
                    if (aGraphQLTypeOutObjectUnion == null) continue;
                    unionGraphQLTypeNames.add(aGraphQLTypeOutObjectUnion.value());
                }

                //Собираем поля
                Set<RGraphQLObjectTypeField> fields = new HashSet<RGraphQLObjectTypeField>();

                //Обрабытываем поля
                for (Field field: classRTypeGraphQL.getDeclaredFields()) {
                    GraphQLField aGraphQLField = field.getAnnotation(GraphQLField.class);
                    if (aGraphQLField ==null) continue;

                    String typeField = getGraphQLType(field.getType(), field.getGenericType());

                    String nameField = field.getName();

                    String externalNameField = aGraphQLField.value();
                    if (externalNameField.trim().length()==0) externalNameField = getExternalName(nameField);

                    fields.add(new RGraphQLObjectTypeField(subsystem, aGraphQLField.auth(), true, typeField, nameField, externalNameField, aGraphQLField.deprecated()));
                }

                //Обрабатываем методы
                for (Method method: classRTypeGraphQL.getDeclaredMethods()) {
                    if (method.isSynthetic()) continue;//Игнорируем генерируемые методы

                    GraphQLTypeMethod aGraphQLTypeMethod = method.getAnnotation(GraphQLTypeMethod.class);
                    if (aGraphQLTypeMethod==null) continue;

                    //TODO Ulitin V. хак, необходимо придумать как нормально обрабатывать такую ситуацию
                    if (method.getReturnType() == Serializable.class) continue;

                    fields.add(buildRGraphQLObjectTypeField(subsystem, classRTypeGraphQL, method, aGraphQLTypeMethod));
                }

                rGraphQLType = new RGraphQLTypeOutObject(name, classRTypeGraphQL.getName(), unionGraphQLTypeNames, fields);
            }

            rTypeGraphQLItems.put(classRTypeGraphQL, rGraphQLType);
        }

        for (Class classRTypeGraphQL : reflections.getTypesAnnotatedWith(GraphQLTypeOutObjectUnion.class, true)) {
            GraphQLTypeOutObjectUnion aGraphQLTypeOutObjectUnion = (GraphQLTypeOutObjectUnion) classRTypeGraphQL.getAnnotation(GraphQLTypeOutObjectUnion.class);

            if (!classRTypeGraphQL.isInterface())
                throw new RuntimeException("Class " + classRTypeGraphQL + " is not interface");

            String name = aGraphQLTypeOutObjectUnion.value();


            //Собираем поля
            Set<RGraphQLObjectTypeField> fields = new HashSet<RGraphQLObjectTypeField>();
            for (Method method : classRTypeGraphQL.getDeclaredMethods()) {
                if (method.isSynthetic()) continue;//Игнорируем генерируемые методы

                GraphQLTypeMethod aGraphQLTypeMethod = method.getAnnotation(GraphQLTypeMethod.class);
                if (aGraphQLTypeMethod == null) continue;

                fields.add(buildRGraphQLObjectTypeField(subsystem, classRTypeGraphQL, method, aGraphQLTypeMethod));
            }


            RGraphQLTypeOutObjectUnion rGraphQLType = new RGraphQLTypeOutObjectUnion(name, fields);
            rTypeGraphQLItems.put(classRTypeGraphQL, rGraphQLType);
        }

        for (Class classRTypeGraphQL : reflections.getTypesAnnotatedWith(GraphQLTypeInput.class)) {
            GraphQLTypeInput aGraphQLTypeInput = (GraphQLTypeInput) classRTypeGraphQL.getAnnotation(GraphQLTypeInput.class);

            String name = aGraphQLTypeInput.value();
            Set<RGraphQLInputObjectTypeField> fields = new HashSet<RGraphQLInputObjectTypeField>();

            //Обрабытываем поля
            for (Field field : classRTypeGraphQL.getDeclaredFields()) {
                GraphQLTypeInput aGraphQLTypeInputField = field.getAnnotation(GraphQLTypeInput.class);
                if (aGraphQLTypeInputField == null) continue;

                String typeField = getGraphQLType(field.getType(), field.getGenericType());

                String nameField = field.getName();

                String externalNameField = aGraphQLTypeInputField.value();
                if (externalNameField.trim().length() == 0) externalNameField = getExternalName(nameField);

                boolean isNotNull = (field.getAnnotation(NotNull.class) != null);

                fields.add(new RGraphQLInputObjectTypeField(typeField, nameField, externalNameField, isNotNull));
            }

            RGraphQLTypeInObject rGraphQLType = new RGraphQLTypeInObject(name, fields);
            rTypeGraphQLItems.put(classRTypeGraphQL, rGraphQLType);
        }

        return rTypeGraphQLItems;
    }

    private static RGraphQLObjectTypeField buildRGraphQLObjectTypeField(String subsystem, Class classRTypeGraphQL, Method method, GraphQLTypeMethod aGraphQLTypeMethod) throws ClassNotFoundException {
        String typeField = getGraphQLType(method.getReturnType(), method.getGenericReturnType());

        String nameMethod = method.getName();

        String externalNameMethod = aGraphQLTypeMethod.value();
        if (externalNameMethod.trim().length() == 0) externalNameMethod = getExternalName(method);

        List<RGraphQLObjectTypeMethodArgument> arguments = new ArrayList<>();
        Class[] parameterTypes = method.getParameterTypes();
        Annotation[][] parametersAnnotations = method.getParameterAnnotations();
        for (int index = 0; index < parameterTypes.length; index++) {
            //Собираем аннотации
            GraphQLSource aGraphQLTarget = null;
            GraphQLName aGraphQLName = null;
            boolean isNotNull = false;
            for (Annotation annotation : parametersAnnotations[index]) {
                if (annotation.annotationType() == GraphQLSource.class) {
                    if (
                            !RemoteObject.instanceOf(parameterTypes[index])) {
                        throw new RuntimeException("Class does not implement interface RemoteObject: " + parameterTypes[index]);
                    }
                    aGraphQLTarget = (GraphQLSource) annotation;
                } else if (annotation.annotationType() == GraphQLName.class) {
                    aGraphQLName = (GraphQLName) annotation;
                } else if (annotation.annotationType() == NotNull.class) {
                    isNotNull = true;
                }
            }
            if (aGraphQLTarget != null) continue;//В эту переменную будет передаваться объект для которого вызывается
            if (aGraphQLName == null) {
                if (parameterTypes[index] == GRequest.class)
                    continue;//В эту переменную будет передаваться окружение
                if (Component.class.isAssignableFrom(parameterTypes[index]))
                    continue;//В эту переменную будет передаваться компонент
                throw new RuntimeException("Not support argument, class: " + classRTypeGraphQL + ", method: " + nameMethod + ", argument index: " + index);
            }

            String typeArgument = getGraphQLType(parameterTypes[index], method.getGenericParameterTypes()[index]);
            String nameArgument = aGraphQLName.value();

            arguments.add(new RGraphQLObjectTypeMethodArgument(typeArgument, nameArgument, nameArgument, isNotNull));
        }

        return new RGraphQLObjectTypeField(subsystem, aGraphQLTypeMethod.auth(), false, typeField, nameMethod, externalNameMethod, aGraphQLTypeMethod.deprecated(), arguments);
    }

    private static String getExternalName(Method method) {
        String nameMethod = method.getName();
        if (nameMethod.startsWith("get") || nameMethod.startsWith("set")) {
            nameMethod = nameMethod.substring(3);
        } else if (nameMethod.startsWith("is")) {
            nameMethod = nameMethod.substring(2);
        }
        return getExternalName(nameMethod);
    }

    private static String getExternalName(String name){
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
    }

    private static String getGraphQLType(Class clazz, Type genericType) throws ClassNotFoundException {
        if (clazz==boolean.class || clazz==Boolean.class){
            return "boolean";
        } else if (clazz==long.class || clazz==Long.class){
            return "long";
        } else if (clazz==int.class || clazz==Integer.class){
            return "int";
        } else if (clazz==double.class || clazz==Double.class){
            return "bigdecimal";
        } else if (clazz==float.class || clazz==Float.class){
            return "float";
        } else if (clazz==String.class) {
            return "string";
        } else if (clazz==Date.class) {
            return "date";
        } else if (clazz==List.class) {
            String genericTypeName = ((ParameterizedType) genericType).getActualTypeArguments()[0].getTypeName();
            Class clazzGenericType = Class.forName(genericTypeName, true, Thread.currentThread().getContextClassLoader());
            return "list:" + getGraphQLType(clazzGenericType, null);
        } else if (clazz == GOptional.class) {
            Type iGenericType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            if (iGenericType instanceof ParameterizedType) {
                ParameterizedType iPGenericType = (ParameterizedType) iGenericType;
                return getGraphQLType((Class) iPGenericType.getRawType(), iPGenericType);
            } else {
                return getGraphQLType((Class) iGenericType, null);
            }
        } else {
            //Возможно это сложный GraphQL объект

            //Возможно обертка
            GraphQLTypeWrapper aGraphQLTypeWrapper = (GraphQLTypeWrapper) clazz.getAnnotation(GraphQLTypeWrapper.class);
            if (aGraphQLTypeWrapper != null) {
                Type iGenericType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                if (iGenericType instanceof ParameterizedType) {
                    ParameterizedType iPGenericType = (ParameterizedType) iGenericType;
                    return getGraphQLType((Class) iPGenericType.getRawType(), iPGenericType);
                } else {
                    return getGraphQLType((Class) iGenericType, null);
                }
            }

            GraphQLTypeInput aGraphQLTypeInput = (GraphQLTypeInput) clazz.getAnnotation(GraphQLTypeInput.class);
            if (aGraphQLTypeInput!=null) return aGraphQLTypeInput.value();

            GraphQLTypeOutObject aGraphQLType = (GraphQLTypeOutObject) clazz.getAnnotation(GraphQLTypeOutObject.class);
            if (aGraphQLType != null) return aGraphQLType.value();

            GraphQLTypeOutObjectUnion aGraphQLTypeOutUnion = (GraphQLTypeOutObjectUnion) clazz.getAnnotation(GraphQLTypeOutObjectUnion.class);
            if (aGraphQLTypeOutUnion != null) return aGraphQLTypeOutUnion.value();

            throw new RuntimeException("Not support type: " + clazz);
        }
    }
}
