package com.infomaximum.cluster.graphql.executor.subscription;

import com.infomaximum.cluster.graphql.remote.graphql.subscribe.RControllerGraphQLSubscribe;
import com.infomaximum.cluster.graphql.struct.GSubscribeEvent;
import com.infomaximum.cluster.graphql.utils.SubscribeKeyUtils;
import com.infomaximum.cluster.struct.Component;

import java.io.Serializable;

public class GraphQLSubscribeEvent {

    private final Component component;

    public GraphQLSubscribeEvent(Component component) {
        this.component = component;
    }

    public void pushEvent(GSubscribeEvent subscribeEvent) {
        GSubscribeEvent.SubscribeValue subscribeValue = subscribeEvent.getSubscribeValue();
        String fullSubscribeKey = SubscribeKeyUtils.getFullSubscribeKey(component.getInfo().getUuid(), subscribeValue.subscribeKey);
        for (RControllerGraphQLSubscribe controller : component.getRemotes().getControllers(RControllerGraphQLSubscribe.class)) {
            controller.pushEvent(fullSubscribeKey, (Serializable) subscribeValue.value);
        }
    }
}
