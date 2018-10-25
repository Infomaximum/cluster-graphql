package com.infomaximum.cluster.graphql.subscription;

import graphql.ExecutionResult;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;

public class SingleSubscriber implements Subscriber {

    private final CompletableFuture<ExecutionResult> completableFuture;

    private Subscription subscription;

    public SingleSubscriber() {
        this.completableFuture = new CompletableFuture<ExecutionResult>();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(Object nextValue) {
        subscription.cancel();//Сразу же отписываемся
        completableFuture.complete((ExecutionResult) nextValue);
    }

    @Override
    public void onError(Throwable throwable) {
        subscription.cancel();
        completableFuture.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
    }

    public CompletableFuture<ExecutionResult> getCompletableFuture() {
        return completableFuture;
    }
}
