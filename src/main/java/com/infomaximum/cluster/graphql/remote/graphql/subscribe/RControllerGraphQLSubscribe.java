package com.infomaximum.cluster.graphql.remote.graphql.subscribe;

import com.infomaximum.cluster.core.remote.struct.RController;

import java.io.Serializable;

/**
 * Created by kris on 02.11.16.
 */
public interface RControllerGraphQLSubscribe extends RController {

    void pushEvent(String subscribeKey, Serializable value);

}
