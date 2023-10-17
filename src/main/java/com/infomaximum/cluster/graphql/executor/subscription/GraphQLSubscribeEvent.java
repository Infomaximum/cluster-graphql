package com.infomaximum.cluster.graphql.executor.subscription;

import com.infomaximum.cluster.graphql.remote.graphql.subscribe.RControllerGraphQLSubscribe;
import com.infomaximum.cluster.graphql.struct.GSubscribeEvent;
import com.infomaximum.cluster.graphql.struct.subscribe.SubscribeKey;
import com.infomaximum.cluster.struct.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphQLSubscribeEvent {

    private final static Logger log = LoggerFactory.getLogger(GraphQLSubscribeEvent.class);

    private final Component component;

    public GraphQLSubscribeEvent(Component component) {
        this.component = component;
    }

    public void pushEvent(GSubscribeEvent subscribeEvent) {
        GSubscribeEvent.SubscribeValue subscribeValue = subscribeEvent.getSubscribeValue();
        SubscribeKey subscribeKey = new SubscribeKey(component, subscribeValue.subscribeKey);
        for (RControllerGraphQLSubscribe controller : component.getRemotes().getControllers(RControllerGraphQLSubscribe.class)) {
            try {
                controller.pushEvent(subscribeKey, subscribeValue.value);
            } catch (Exception e) {
                log.error("Error push event", e);
            }
        }
    }
}
