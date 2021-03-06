package com.infomaximum.cluster.graphql.utils;

public class ExceptionUtils {

    public static RuntimeException coercionRuntimeException(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            return (RuntimeException) throwable;
        } else {
            return new RuntimeException(throwable);
        }
    }

}
