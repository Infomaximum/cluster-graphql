package com.infomaximum.cluster.graphql.instrumentation;

import graphql.execution.instrumentation.InstrumentationState;

public class QueryInstrumentationState implements InstrumentationState {

    public final long id;

    public QueryInstrumentationState(long id) {
        this.id = id;
    }
}
