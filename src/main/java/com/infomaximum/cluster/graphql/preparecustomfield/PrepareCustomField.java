package com.infomaximum.cluster.graphql.preparecustomfield;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.exception.GraphQLExecutorDataFetcherException;
import com.infomaximum.cluster.graphql.struct.GRequest;

import java.io.Serializable;
import java.lang.reflect.Type;

public interface PrepareCustomField<T> {

    boolean isSupport(Class clazz);

    Type getEndType(Type genericType);

    Serializable prepare(String keyFieldRequest, T value);

    //TODO Когда для построения иерархии перейдем на классы необходимо заменить на Serializable
    Object execute(GRequest request, String keyFieldRequest, RemoteObject source) throws GraphQLExecutorDataFetcherException;
}