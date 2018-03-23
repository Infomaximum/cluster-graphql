package com.infomaximum.cluster.graphql.schema.build.graphqltype;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
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
import com.infomaximum.cluster.graphql.schema.struct.out.union.RGraphQLTypeOutObjectUnion;
import com.infomaximum.cluster.graphql.struct.GOptional;
import com.infomaximum.cluster.struct.Component;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

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

	private final GraphQLSchemaType graphQLSchemaType;

	private Set<PrepareCustomField> prepareCustomFields;
	private TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder;

	public TypeGraphQLBuilder(Component component, GraphQLSchemaType graphQLSchemaType) {
		this.componentUuid = component.getInfo().getUuid();
		this.packageName = componentUuid;

		this.graphQLSchemaType = graphQLSchemaType;
	}

	public TypeGraphQLBuilder(String packageName, GraphQLSchemaType graphQLSchemaType) {
		this.componentUuid = null;
		this.packageName = packageName;

		this.graphQLSchemaType = graphQLSchemaType;
	}

	public TypeGraphQLBuilder withCustomFields(Set<PrepareCustomField> prepareCustomFields){
		this.prepareCustomFields = prepareCustomFields;
		return this;
	}

	public TypeGraphQLBuilder withFieldConfigurationBuilder(TypeGraphQLFieldConfigurationBuilder fieldConfigurationBuilder){
		this.fieldConfigurationBuilder=fieldConfigurationBuilder;
		return this;
	}

	public Map<Class, RGraphQLType> build() throws GraphQLExecutorException {
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
					if (aGraphQLField == null) continue;

					String typeField;
					try {
						typeField = getGraphQLType(field.getGenericType());
					} catch (Exception e) {
						throw new RuntimeException("Exception build type, class: " + classRTypeGraphQL.getName() + ", field: " + field.getName(), e);
					}

					String nameField = field.getName();

					String externalNameField = aGraphQLField.value();
					if (externalNameField.trim().length() == 0)
						externalNameField = GraphQLSchemaType.convertToGraphQLName(nameField);

					RemoteObject fieldConfiguration = null;
					if (fieldConfigurationBuilder!=null) {
						fieldConfiguration = fieldConfigurationBuilder.build(field);
					}

					fields.add(new RGraphQLObjectTypeField(componentUuid, fieldConfiguration, false, true, typeField, nameField, externalNameField, aGraphQLField.deprecated()));
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
					typeField = getGraphQLType(field.getGenericType());
				} catch (Exception e) {
					throw new RuntimeException("Exception build type, class: " + classRTypeGraphQL.getName() + ", field: " + field.getName(), e);
				}

				String nameField = field.getName();

				String externalNameField = aGraphQLTypeInputField.value();
				if (externalNameField.trim().length() == 0)
					externalNameField = GraphQLSchemaType.convertToGraphQLName(nameField);

				boolean isNotNull = (field.getAnnotation(NotNull.class) != null);

				fields.add(new RGraphQLInputObjectTypeField(typeField, nameField, externalNameField, isNotNull));
			}

			RGraphQLTypeInObject rGraphQLType = new RGraphQLTypeInObject(name, fields);
			rTypeGraphQLItems.put(classRTypeGraphQL, rGraphQLType);
		}

		return rTypeGraphQLItems;
	}

	private RGraphQLObjectTypeField buildRGraphQLObjectTypeField(String componentUuid, Class classRTypeGraphQL, Method method, GraphQLField aGraphQLTypeMethod) throws GraphQLExecutorException {
		//Если родительский класс не реализовывает интерфейс RemoteObject, то все его поля могут быть только статическими
		if (!RemoteObject.class.isAssignableFrom(method.getDeclaringClass())) {
			if (!Modifier.isStatic(method.getModifiers())) throw new GraphQLExecutorException("Method " + method.getName() + " in class " + method.getDeclaringClass().getName() + " is not static");
		}

		String typeField;
		try{
			typeField = getGraphQLType(method.getGenericReturnType());
		} catch (Exception e) {
			throw new GraphQLExecutorException("Exception build type, class: " + classRTypeGraphQL.getName() + ", method: " + method.getName(), e);
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
					if (!RemoteObject.class.isAssignableFrom(parameterTypes[index])) {
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
			if (aGraphQLName == null) continue;//В эту переменную будет передаваться внешняя переменная

			String typeArgument;
			try{
				typeArgument = getGraphQLType(method.getGenericParameterTypes()[index]);
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

		boolean isPrepereField = isPrepareField(prepareCustomFields, method.getReturnType());
		if (isPrepereField) {//Prepere поля должны быть обязательно static
			if (!Modifier.isStatic(method.getModifiers())) throw new GraphQLExecutorException("Method " + method.getName() + " in class " + method.getDeclaringClass().getName() + " is not static");
		}

		return new RGraphQLObjectTypeField(componentUuid, fieldConfiguration, isPrepereField, false, typeField, nameMethod, externalNameMethod, aGraphQLTypeMethod.deprecated(), arguments);
	}

	private String getGraphQLType(Type type) throws ClassNotFoundException {
		Class rawType;
		if (type instanceof ParameterizedTypeImpl) {
			rawType = ((ParameterizedTypeImpl) type).getRawType();
		} else if (type instanceof Class){
			rawType = (Class) type;
		} else {
			throw new RuntimeException("Not support type: " + type);
		}

		//Проверяем на скалярный тип объекта
		GraphQLTypeScalar graphQLTypeScalar = graphQLSchemaType.getTypeScalarByClass(rawType);
		if (graphQLTypeScalar != null) return graphQLTypeScalar.getName();

		//Проверяем на коллекцию
		if (rawType == ArrayList.class || rawType == HashSet.class) {
			String genericTypeName = ((ParameterizedType) type).getActualTypeArguments()[0].getTypeName();
			Class clazzGenericType = Class.forName(genericTypeName, true, Thread.currentThread().getContextClassLoader());
			return "collection:" + getGraphQLType(clazzGenericType);
		}

		//Проверяем на GOptional
		if (rawType == GOptional.class) {
			Type iGenericType = ((ParameterizedType) type).getActualTypeArguments()[0];
			return getGraphQLType(iGenericType);
		}

		//Проверяем на input объект
		GraphQLTypeInput aGraphQLTypeInput = (GraphQLTypeInput) rawType.getAnnotation(GraphQLTypeInput.class);
		if (aGraphQLTypeInput != null) return aGraphQLTypeInput.value();

		//Проверяем на out объект
		GraphQLTypeOutObject aGraphQLType = (GraphQLTypeOutObject) rawType.getAnnotation(GraphQLTypeOutObject.class);
		if (aGraphQLType != null) return aGraphQLType.value();

		//Проверяем на union объект
		GraphQLTypeOutObjectUnion aGraphQLTypeOutUnion = (GraphQLTypeOutObjectUnion) rawType.getAnnotation(GraphQLTypeOutObjectUnion.class);
		if (aGraphQLTypeOutUnion != null) return aGraphQLTypeOutUnion.value();

		//Проверяем принадлежность к кастомным полям
		if (prepareCustomFields != null) {
			for (PrepareCustomField customField : prepareCustomFields) {
				if (customField.isSupport(rawType)) {
					return getGraphQLType(customField.getEndType(type));
				}
			}
		}

		throw new RuntimeException("Not support type: " + type);
	}


	private static String getExternalName(Method method) {
		String nameMethod = method.getName();
		if (nameMethod.startsWith("get") || nameMethod.startsWith("set")) {
			nameMethod = nameMethod.substring(3);
		} else if (nameMethod.startsWith("is")) {
			nameMethod = nameMethod.substring(2);
		}
		return GraphQLSchemaType.convertToGraphQLName(nameMethod);
	}


	private static boolean isPrepareField(Set<PrepareCustomField> prepareCustomFields, Class clazz){
		if (prepareCustomFields!=null) {
			for (PrepareCustomField customField: prepareCustomFields) {
				if (customField.isSupport(clazz)) {
					return (customField instanceof PrepareCustomField);
				}
			}
		}
		return false;
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
