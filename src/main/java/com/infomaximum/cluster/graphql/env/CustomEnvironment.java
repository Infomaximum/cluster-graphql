package com.infomaximum.cluster.graphql.env;

import com.infomaximum.cluster.graphql.struct.GRequest;
import com.infomaximum.cluster.struct.Component;

/**
 * Created by user on 06.09.2017.
 */
public interface CustomEnvironment<T> {

    boolean isSupport(Class classType);

    T getValue(GRequest request);
}
