package com.infomaximum.cluster.graphql.struct;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class GSubscribeEvent<T extends Serializable> {

    public class SubscribeValue<T extends Serializable> implements RemoteObject {

        public final byte[] subscribeKey;
        public final Optional<T> value;
        public final CompletableFuture subscribeFuture;

        public SubscribeValue(byte[] subscribeKey, T value, CompletableFuture<?> subscribeFuture) {
            this.subscribeKey = subscribeKey;
            this.value = Optional.ofNullable(value);
            this.subscribeFuture = subscribeFuture;
        }
    }

    private final SubscribeValue<T> value;

    public GSubscribeEvent(byte[] subscribeKey, T value) {
        this(subscribeKey, value, CompletableFuture.completedFuture(Optional.ofNullable(value)));
    }

    public GSubscribeEvent(byte[] subscribeKey, T value, CompletableFuture<?> subscribeFuture) {
        this.value = new SubscribeValue(subscribeKey, value, subscribeFuture);
    }

    public SubscribeValue<T> getSubscribeValue() {
        return value;
    }
}
