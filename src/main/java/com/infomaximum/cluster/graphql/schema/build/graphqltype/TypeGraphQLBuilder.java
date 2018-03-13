package com.infomaximum.cluster.graphql.schema.build.graphqltype;

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
import com.infomaximum.cluster.struct.Component;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * Created by kris on 30.12.16.
 */
public class TypeGraphQLBuilder {

	private final static Logger log = LoggerFactory.getLogger(TypeGraphQLBuilder.class);

	private final String componentUuid;
	private final String packageName;

	private TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder;

	public TypeGraphQLBuilder(Component component) {
		this.componentUuid = component.getInfo().getUuid();
		this.packageName = componentUuid;
	}

	public TypeGraphQLBuilder(String packageName) {
		this.componentUuid = null;
		this.packageName = packageName;
	}

	public TypeGraphQLBuilder withFieldConfigurationBuilder(TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder){
		this.fieldConfigurationBuilder=fieldConfigurationBuilder;
		return this;
	}

	public Map<Class, RGraphQLType> build() throws ClassNotFoundException {
		Reflections reflections = new Reflections(packageName);

		Map<Class, RGraphQLType> rTypeGraphQLItems = new HashMap<Class, RGraphQLType>();
		for (Class classRTypeGraphQL: reflections.getTypesAnnotatedWith(GraphQLTypeOutObject.class, true)) {
			GraphQLTypeOutObject aGraphQLType = (GraphQLTypeOutObject) classRTypeGraphQL.getAnnotation(GraphQLTypeOutObject.class);

			//Имя типа
			String name = aGraphQLType.value();

			RGraphQLType rGraphQLType;
			if (classRTypeGraphQL.isEnum()) {
				Set<String> enumValues = null;
				enumValues = new HashSet<String>();
				for (Object oEnum : classRTypeGraphQL.getEnumConstants()) {
					enumValues.add(((Enum) oEnum).name());
				}

				rGraphQLType = new RGraphQLTypeEnum(name, enumValues);
			} else {
				//Собираем union типы
				Set<String> unionGraphQLTypeNames = new HashSet<String>();
				findUnionGraphQLTypeNames(classRTypeGraphQL, unionGraphQLTypeNames);

				//Собираем поля
				Set<RGraphQLObjectTypeField> fields = new HashSet<RGraphQLObjectTypeField>();

				//Обрабытываем поля
				for (Field field: classRTypeGraphQL.getDeclaredFields()) {
					GraphQLField aGraphQLField = field.getAnnotation(GraphQLField.class);
					if (aGraphQLField ==null) continue;

					String typeField;
					try {
						typeField = getGraphQLType(field.getType(), field.getGenericType());
					} catch (Exception e) {
						throw new RuntimeException("Exception build type, class: " + classRTypeGraphQL.getName() + ", field: " + field.getName(), e);
					}

					String nameField = field.getName();

					String externalNameField = aGraphQLField.value();
					if (externalNameField.trim().length() == 0) externalNameField = getExternalName(nameField);

					RemoteObject fieldConfiguration = null;
					if (fieldConfigurationBuilder!=null) {
						fieldConfiguration = fieldConfigurationBuilder.build(field);
					}

					fields.add(new RGraphQLObjectTypeField(componentUuid, fieldConfiguration, true, typeField, nameField, externalNameField, aGraphQLField.deprecated()));
				}

				//Обрабатываем методы
				for (Method method : classRTypeGraphQL.getMethods()) {
					if (method.isSynthetic()) continue;//Игнорируем генерируемые методы

					GraphQLField aGraphQLTypeMethod = method.getAnnotation(GraphQLField.class);
					if (aGraphQLTypeMethod == null) continue;

					fields.add(buildRGraphQLObjectTypeField(componentUuid, classRTypeGraphQL, method, aGraphQLTypeMethod));
				}

				rGraphQLType = new RGraphQLTypeOutObject(name, classRTypeGraphQL.getName(), unionGraphQLTypeNames, fields);
			}

			rTypeGraphQLItems.put(classRTypeGraphQL, rGraphQLType);
		}

		for (Class classRTypeGraphQL : reflections.getTypesAnnotatedWith(GraphQLTypeOutObjectUnion.class, true)) {
			GraphQLTypeOutObjectUnion aGraphQLTypeOutObjectUnion = (GraphQLTypeOutObjectUnion) classRTypeGraphQL.getAnnotation(GraphQLTypeOutObjectUnion.class);

			if (!(classRTypeGraphQL.isInterface() || Modifier.isAbstract(classRTypeGraphQL.getModifiers()))) {
				throw new RuntimeException("Class " + classRTypeGraphQL + " is not interface | abstract class");
			}

			String name = aGraphQLTypeOutObjectUnion.value();


			//Собираем поля
			Set<RGraphQLObjectTypeField> fields = new HashSet<RGraphQLObjectTypeField>();
			for (Method method : classRTypeGraphQL.getMethods()) {
				if (method.isSynthetic()) continue;//Игнорируем генерируемые методы

				GraphQLField aGraphQLTypeMethod = method.getAnnotation(GraphQLField.class);
				if (aGraphQLTypeMethod == null) continue;

				fields.add(buildRGraphQLObjectTypeField(componentUuid, classRTypeGraphQL, method, aGraphQLTypeMethod));
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

				String typeField;
				try{
					typeField = getGraphQLType(field.getType(), field.getGenericType());
				} catch (Exception e) {
					throw new RuntimeException("Exception build type, class: " + classRTypeGraphQL.getName() + ", field: " + field.getName(), e);
				}

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

	private RGraphQLObjectTypeField buildRGraphQLObjectTypeField(String componentUuid, Class classRTypeGraphQL, Method method, GraphQLField aGraphQLTypeMethod) throws ClassNotFoundException {
		String typeField;
		try{
			typeField = getGraphQLType(method.getReturnType(), method.getGenericReturnType());
		} catch (Exception e) {
			throw new RuntimeException("Exception build type, class: " + classRTypeGraphQL.getName() + ", method: " + method.getName(), e);
		}

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
					//TODO Ulitin V. Как только избавимся от RemoteObjectOld сразу включить - этот контроль обязателен!!!
//					if (
//							!RemoteObject.class.isAssignableFrom(parameterTypes[index])) {
//						throw new RuntimeException("Class does not implement interface RemoteObject: " + parameterTypes[index]);
//					}
					aGraphQLTarget = (GraphQLSource) annotation;
				} else if (annotation.annotationType() == GraphQLName.class) {
					aGraphQLName = (GraphQLName) annotation;
				} else if (annotation.annotationType() == NotNull.class) {
					isNotNull = true;
				}
			}
			if (aGraphQLTarget != null) continue;//В эту переменную будет передаваться объект для которого вызывается
			if (aGraphQLName == null) continue;//В эту переменную будет передаваться внешняя переменная

			String typeArgument;
			try{
				typeArgument = getGraphQLType(parameterTypes[index], method.getGenericParameterTypes()[index]);
			} catch (Exception e) {
				throw new RuntimeException("Exception build type, class: " + classRTypeGraphQL.getName() + ", method: " + method.getName(), e);
			}
			String nameArgument = aGraphQLName.value();

			arguments.add(new RGraphQLObjectTypeMethodArgument(typeArgument, nameArgument, nameArgument, isNotNull));
		}

		RemoteObject fieldConfiguration = null;
		if (fieldConfigurationBuilder!=null) {
			fieldConfiguration = fieldConfigurationBuilder.build(method);
		}

		return new RGraphQLObjectTypeField(componentUuid, fieldConfiguration, false, typeField, nameMethod, externalNameMethod, aGraphQLTypeMethod.deprecated(), arguments);
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

	private static String getExternalName(String name) {
		return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
	}

	private static String getGraphQLType(Class clazz, Type genericType) throws ClassNotFoundException {
		if (clazz == boolean.class || clazz == Boolean.class) {
			return "boolean";
		} else if (clazz == long.class || clazz == Long.class) {
			return "long";
		} else if (clazz == int.class || clazz == Integer.class) {
			return "int";
		} else if (clazz == double.class || clazz == Double.class) {
			return "bigdecimal";
		} else if (clazz == float.class || clazz == Float.class) {
			return "float";
		} else if (clazz == String.class) {
			return "string";
		} else if (clazz == Date.class) {
			return "date";
		} else if (clazz == ArrayList.class || clazz == HashSet.class) {
			String genericTypeName = ((ParameterizedType) genericType).getActualTypeArguments()[0].getTypeName();
			Class clazzGenericType = Class.forName(genericTypeName, true, Thread.currentThread().getContextClassLoader());
			return "collection:" + getGraphQLType(clazzGenericType, null);
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
			if (aGraphQLTypeInput != null) return aGraphQLTypeInput.value();

			GraphQLTypeOutObject aGraphQLType = (GraphQLTypeOutObject) clazz.getAnnotation(GraphQLTypeOutObject.class);
			if (aGraphQLType != null) return aGraphQLType.value();

			GraphQLTypeOutObjectUnion aGraphQLTypeOutUnion = (GraphQLTypeOutObjectUnion) clazz.getAnnotation(GraphQLTypeOutObjectUnion.class);
			if (aGraphQLTypeOutUnion != null) return aGraphQLTypeOutUnion.value();

			throw new RuntimeException("Not support type: " + clazz);
		}
	}

	/**
	 * Вытаскиваем у класса всех родителей - union
	 * @param classRTypeGraphQL
	 * @param unionGraphQLTypeNames
	 */
	private static void findUnionGraphQLTypeNames(Class classRTypeGraphQL, Set<String> unionGraphQLTypeNames) {
		GraphQLTypeOutObjectUnion aGraphQLTypeOutObjectUnion = (GraphQLTypeOutObjectUnion) classRTypeGraphQL.getAnnotation(GraphQLTypeOutObjectUnion.class);
		if (aGraphQLTypeOutObjectUnion != null) {
			unionGraphQLTypeNames.add(aGraphQLTypeOutObjectUnion.value());
		}

		for (Class iClass : classRTypeGraphQL.getInterfaces()) {
			GraphQLTypeOutObjectUnion iAGraphQLTypeOutObjectUnion = (GraphQLTypeOutObjectUnion) iClass.getAnnotation(GraphQLTypeOutObjectUnion.class);
			if (iAGraphQLTypeOutObjectUnion == null) continue;
			unionGraphQLTypeNames.add(iAGraphQLTypeOutObjectUnion.value());
		}

		Class superClass = classRTypeGraphQL.getSuperclass();
		if (superClass==null) return;

		findUnionGraphQLTypeNames(superClass, unionGraphQLTypeNames);
	}
}
