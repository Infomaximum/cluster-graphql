package com.infomaximum.cluster.graphql.struct.subscribe;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.struct.Component;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class SubscribeKey implements RemoteObject {

    private final byte[] key;

    public SubscribeKey(Component component, byte[] subscribeKey) {
        this(component.getUniqueId(), subscribeKey);
    }

    public SubscribeKey(int componentUniqueId, byte[] subscribeKey) {
        key = ByteBuffer.allocate(Integer.BYTES + subscribeKey.length)
                .putInt(componentUniqueId)
                .put(subscribeKey)
                .array();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SubscribeKey)) {
            return false;
        }
        return Arrays.equals(key, ((SubscribeKey) other).key);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }
}
