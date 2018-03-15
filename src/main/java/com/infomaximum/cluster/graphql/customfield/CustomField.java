package com.infomaximum.cluster.graphql.customfield;

import com.infomaximum.cluster.struct.Component;

import java.lang.reflect.Type;

public interface CustomField<T> {

    boolean isSupport(Class clazz);

    Type getEndType(Type genericType);

    Object getEndValue(Component component, Object source, T value);
}
