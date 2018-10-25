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

    public GSubscribeEvent(T value) {
        this.value = new SubscribeValue(getSubscribeKey(), value);
    }

    public SubscribeValue<T> getSubscribeValue() {
        return value;
    }

    protected abstract String getSubscribeKey();
}
