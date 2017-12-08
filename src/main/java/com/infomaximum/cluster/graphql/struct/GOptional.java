package com.infomaximum.cluster.graphql.struct;

/**
 * Created by kris on 06.03.17.
 */
public final class GOptional<T> {

    private final T value;
    private final boolean isPresent;

    public GOptional(T value, boolean isPresent) {
        this.value = value;
        this.isPresent = isPresent;
    }

    public T get() {
        return value;
    }

    public boolean isPresent() {
        return isPresent;
    }
}
