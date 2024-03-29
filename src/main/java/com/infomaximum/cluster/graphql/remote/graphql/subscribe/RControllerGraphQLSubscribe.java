package com.infomaximum.cluster.graphql.remote.graphql.subscribe;

import com.infomaximum.cluster.core.remote.struct.RController;
import com.infomaximum.cluster.graphql.struct.subscribe.SubscribeKey;

import java.io.Serializable;
import java.util.Optional;

/**
 * Created by kris on 02.11.16.
 */
public interface RControllerGraphQLSubscribe extends RController {

    void pushEvent(SubscribeKey subscribeKey, Optional<? extends Serializable> value) throws Exception;

}
