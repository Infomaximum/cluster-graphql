package com.infomaximum.cluster.graphql.executor.subscription;

import com.infomaximum.cluster.graphql.struct.subscribe.SubscribeKey;
import io.reactivex.ObservableEmitter;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class GraphQLSubscribeEngineImpl implements GraphQLSubscribeEngine {

    private final ConcurrentMap<SubscribeKey, CopyOnWriteArraySet<ObservableEmitter>> subscriber;

    public GraphQLSubscribeEngineImpl() {
        this.subscriber = new ConcurrentHashMap<>();
    }

    public void pushEvent(SubscribeKey subscribeKey, Optional<? extends Serializable> value) {
        CopyOnWriteArraySet<ObservableEmitter> observables = subscriber.get(subscribeKey);
        if (observables == null || observables.isEmpty()) return;
        for (ObservableEmitter emitter : observables) {
            emitter.onNext(value);
        }
    }

    public void addListener(int componentUniqueId, byte[] bSubscribeKey, ObservableEmitter observable) {
        SubscribeKey subscribeKey = new SubscribeKey(componentUniqueId, bSubscribeKey);
        CopyOnWriteArraySet<ObservableEmitter> observables = subscriber.computeIfAbsent(subscribeKey, s -> new CopyOnWriteArraySet<ObservableEmitter>());
        observables.add(observable);

        observable.setCancellable(() -> {
            observables.remove(observable);
        });
    }

}
