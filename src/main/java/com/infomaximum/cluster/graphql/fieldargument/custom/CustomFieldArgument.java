package com.infomaximum.cluster.graphql.fieldargument.custom;

import com.infomaximum.cluster.graphql.struct.GRequest;

/**
 * Created by user on 06.09.2017.
 */
public interface CustomFieldArgument<T> {

    boolean isSupport(Class classType);

    T getValue(GRequest request, Class classType);
}
