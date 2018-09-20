package com.infomaximum.cluster.graphql.struct;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;

/**
 * Created by kris on 23.01.17.
 */
public class GRequest implements RemoteObject {

    public static class RemoteAddress implements RemoteObject {

        public final String rawRemoteAddress;
        public final String endRemoteAddress;

        public RemoteAddress(String remoteAddress) {
            this.rawRemoteAddress = remoteAddress;
            this.endRemoteAddress = remoteAddress;
        }

        public RemoteAddress(String rawRemoteAddress, String endRemoteAddress) {
            this.rawRemoteAddress = rawRemoteAddress;
            this.endRemoteAddress = endRemoteAddress;
        }
    }

    private final Instant instant;

    private final RemoteAddress remoteAddress;

    private final String query;
    private final HashMap<String, Serializable> queryVariables;

    public GRequest(
            Instant instant,
            RemoteAddress remoteAddress,
            String query, HashMap<String, Serializable> queryVariables
    ) {
        this.instant = instant;

        this.remoteAddress = remoteAddress;

        this.query = query;
        this.queryVariables = queryVariables;
    }

    public Instant getInstant() {
        return instant;
    }

    public RemoteAddress getRemoteAddress() {
        return remoteAddress;
    }

    public String getQuery() {
        return query;
    }

    public HashMap<String, Serializable> getQueryVariables() {
        return queryVariables;
    }

}
