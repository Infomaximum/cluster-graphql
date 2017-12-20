package com.infomaximum.cluster.graphql.customtype;

import com.infomaximum.cluster.graphql.struct.GRequest;

/**
 * Created by user on 06.09.2017.
 */
public interface CustomEnvType<T> {

    boolean isSupport(Class classType);

    T getValue(GRequest request, Class classType);
}
