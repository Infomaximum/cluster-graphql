package com.infomaximum.cluster.graphql.subscription;

import com.infomaximum.cluster.graphql.executor.struct.GExecutionResult;
import graphql.ExecutionResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public class SingleSubscriber implements Flow.Subscriber {

    private final CompletableFuture<GExecutionResult> completableFuture;

    private Flow.Subscription subscription;

    public SingleSubscriber() {
        this.completableFuture = new CompletableFuture<GExecutionResult>();
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(Object nextValue) {
        subscription.cancel();//Сразу же отписываемся
        completableFuture.complete(new GExecutionResult((ExecutionResult) nextValue));
    }

    @Override
    public void onError(Throwable throwable) {
        subscription.cancel();
        completableFuture.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
    }

    public CompletableFuture<GExecutionResult> getCompletableFuture() {
        return completableFuture;
    }
}
