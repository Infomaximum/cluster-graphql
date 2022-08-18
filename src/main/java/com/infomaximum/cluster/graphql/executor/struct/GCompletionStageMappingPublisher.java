package com.infomaximum.cluster.graphql.executor.struct;

import graphql.execution.reactive.CompletionStageMappingPublisher;
import org.reactivestreams.FlowAdapters;

import java.util.concurrent.Flow;

public class GCompletionStageMappingPublisher<D, U> implements Flow.Publisher<D> {

    private final CompletionStageMappingPublisher csmp;

    public GCompletionStageMappingPublisher(CompletionStageMappingPublisher csmp) {
        this.csmp = csmp;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super D> subscriber) {
        csmp.subscribe(FlowAdapters.toSubscriber(subscriber));
    }
}
