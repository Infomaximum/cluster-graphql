package com.infomaximum.cluster.graphql.schema.build.graphqltype;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface TypeGraphQLFieldConfigurationBuilder<T extends RemoteObject> {

    T build(Method method);

    T build(Field field);
}
