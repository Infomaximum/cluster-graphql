package com.infomaximum.cluster.graphql.preparecustomfield;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.struct.ContextRequest;
import com.infomaximum.cluster.struct.Component;

import java.io.Serializable;
import java.lang.reflect.Type;

public interface PrepareCustomField<T> {

    boolean isSupport(Class clazz);

    Type getEndType(Type genericType);

    Serializable requestPrepare(Component component, String keyField, T value, ContextRequest context);

    Serializable execute(String keyField, RemoteObject source, ContextRequest context) throws GraphQLExecutorDataFetcherException;

    void requestCompleted(ContextRequest context);
}