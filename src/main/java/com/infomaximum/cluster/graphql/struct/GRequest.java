package com.infomaximum.cluster.graphql.struct;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;

import javax.servlet.http.Cookie;
import java.io.Serializable;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by kris on 23.01.17.
 */
public class GRequest implements RemoteObject {

    private static final AtomicLong uuids = new AtomicLong();

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

    private final RemoteAddress remoteAddress;

    private final String query;
    private final HashMap<String, Serializable> queryVariables;

    private final HashMap<String, String[]> parameters;

    private final Cookie[] cookies;

    private final ArrayList<UploadFile> uploadFiles;

    public GRequest(
            String frontendComponentKey,
            Instant instant,
            RemoteAddress remoteAddress,
            String query, HashMap<String, Serializable> queryVariables,
            HashMap<String, String[]> parameters, Cookie[] cookies,
            ArrayList<UploadFile> uploadFiles
    ) {
        this.instant = instant;

        this.uuid = new StringBuilder().append(frontendComponentKey).append(':').append(uuids.incrementAndGet()).toString();

        this.frontendComponentKey = frontendComponentKey;

        this.remoteAddress = remoteAddress;

        this.query = query;
        this.queryVariables = queryVariables;

        this.parameters = parameters;
        this.cookies = cookies;

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

    public RemoteAddress getRemoteAddress() {
        return remoteAddress;
    }

    public String getQuery() {
        return query;
    }

    public HashMap<String, Serializable> getQueryVariables() {
        return queryVariables;
    }

    public String getParameter(String name) {
        String[] values = getParameters(name);
        return (values == null) ? null : values[0];
    }

    public String[] getParameters(String name) {
        return parameters.get(name);
    }

    public Cookie getCookie(String name) {
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) return cookie;
            }
        }
        return null;
    }

    public ArrayList<UploadFile> getUploadFiles() {
        return uploadFiles;
    }

}
