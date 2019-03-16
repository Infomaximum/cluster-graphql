package com.infomaximum.cluster.graphql.struct;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;

import java.io.Serializable;

public abstract class GSubscribeEvent<T extends Serializable> {

    public class SubscribeValue<T> implements RemoteObject {

        public final String subscribeKey;
        public final T value;

        public SubscribeValue(String subscribeKey, T value) {
            this.subscribeKey = subscribeKey;
            this.value = value;
        }
    }

    private final SubscribeValue<T> value;

    public GSubscribeEvent(String subscribeKey, T value) {
        this.value = new SubscribeValue(subscribeKey, value);
    }

    public SubscribeValue<T> getSubscribeValue() {
        return value;
    }
}
