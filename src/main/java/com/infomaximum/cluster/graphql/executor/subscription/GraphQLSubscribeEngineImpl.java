package com.infomaximum.cluster.graphql.executor.subscription;

import com.infomaximum.cluster.graphql.utils.SubscribeKeyUtils;
import io.reactivex.ObservableEmitter;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class GraphQLSubscribeEngineImpl implements GraphQLSubscribeEngine {

    private final ConcurrentMap<String, CopyOnWriteArraySet<ObservableEmitter>> subscriber;

    public GraphQLSubscribeEngineImpl() {
        this.subscriber = new ConcurrentHashMap<>();
    }

    public void pushEvent(String subscribeKey, Serializable value) {
        CopyOnWriteArraySet<ObservableEmitter> observables = subscriber.get(subscribeKey);
        if (observables == null || observables.isEmpty()) return;
        for (ObservableEmitter emitter : observables) {
            emitter.onNext(value);
        }
    }

    public void addListener(String componentUuid, String subscribeKey, ObservableEmitter observable) {
        String fullSubscribeKey = SubscribeKeyUtils.getFullSubscribeKey(componentUuid, subscribeKey);
        CopyOnWriteArraySet<ObservableEmitter> observables = subscriber.computeIfAbsent(fullSubscribeKey, s -> new CopyOnWriteArraySet<ObservableEmitter>());
        observables.add(observable);

        observable.setCancellable(() -> {
            observables.remove(observable);
        });
    }

}
