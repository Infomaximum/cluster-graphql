package com.infomaximum.cluster.graphql.utils;

public class SubscribeKeyUtils {

    public static String getFullSubscribeKey(String componentUuid, String subscribeKey) {
        return new StringBuilder(componentUuid.length() + subscribeKey.length() + 1)
                .append(componentUuid)
                .append(':')
                .append(subscribeKey)
                .toString();
    }
}
