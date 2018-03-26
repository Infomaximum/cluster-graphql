package com.infomaximum.cluster.graphql.preparecustomfield;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.struct.GRequest;

import java.io.Serializable;
import java.lang.reflect.Type;

public interface PrepareCustomField<T> {

    boolean isSupport(Class clazz);

    Type getEndType(Type genericType);

    Serializable prepare(GRequest request, String keyField, T value);

    void prepareException(GRequest request, Throwable throwable);

    Serializable execute(GRequest request, String keyField, RemoteObject source) throws GraphQLExecutorDataFetcherException;

    void requestCompleted(GRequest request, Throwable throwable);
}