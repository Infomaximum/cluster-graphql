package com.infomaximum.cluster.graphql.struct;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;

import java.io.Serializable;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by kris on 23.01.17.
 */
public class GRequest<C extends RemoteObject> implements RemoteObject {

    private static final AtomicLong uuids = new AtomicLong();

    public static class UploadFile implements RemoteObject {
        public final String fieldname;
        public final String filename;
        public final URI uri;

        public UploadFile(String fieldname, String filename, URI uri) {
            this.fieldname = fieldname;
            this.filename = filename;
            this.uri = uri;
        }
    }

    private final Instant instant;

    private final String uuid;

    private final String frontendComponentKey;

    private final String rawRemoteAddress;
    private final String endRemoteAddress;

    private final C requestContext;

    private HashMap<String, Serializable> externalVariables;
    private ArrayList<UploadFile> uploadFiles;

    public GRequest(
            String frontendComponentKey,
            Instant instant,
            String rawRemoteAddress,
            String endRemoteAddress,
            C requestContext,
            HashMap<String, Serializable> externalVariables,
            ArrayList<UploadFile> uploadFiles
    ) {
        this.instant = instant;

        this.uuid = new StringBuilder().append(frontendComponentKey).append(':').append(uuids.incrementAndGet()).toString();

        this.frontendComponentKey = frontendComponentKey;

        this.rawRemoteAddress = rawRemoteAddress;
        this.endRemoteAddress = endRemoteAddress;

        this.requestContext = requestContext;

        this.externalVariables = externalVariables;
        this.uploadFiles = uploadFiles;
    }

    public Instant getInstant() {
        return instant;
    }

    public String getUuid() {
        return uuid;
    }

    public String getFrontendComponentKey() {
        return frontendComponentKey;
    }

    public String getRawRemoteAddress() {
        return rawRemoteAddress;
    }

    public String getEndRemoteAddress() {
        return endRemoteAddress;
    }

    public C getRequestContext() {
        return requestContext;
    }

    public HashMap<String, Serializable> getExternalVariables() {
        return externalVariables;
    }

    public ArrayList<UploadFile> getUploadFiles() {
        return uploadFiles;
    }

}
