package com.infomaximum.cluster.graphql.struct;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.struct.Component;

import java.io.Serializable;
import java.net.URI;
import java.util.*;

/**
 * Created by kris on 23.01.17.
 */
public class GRequest<C extends RemoteObject> implements RemoteObject {

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

    private final String frontendComponentKey;
    private final String remoteAddress;
    private final C requestContext;

    private HashMap<String, Serializable> externalVariables;
    private ArrayList<UploadFile> uploadFiles;

    public GRequest(
            String frontendComponentKey,
            String remoteAddress,
            C requestContext,
            HashMap<String, Serializable> externalVariables,
            ArrayList<UploadFile> uploadFiles
    ) {
        this.frontendComponentKey = frontendComponentKey;
        this.remoteAddress = remoteAddress;
        this.requestContext = requestContext;

        this.externalVariables = externalVariables;
        this.uploadFiles = uploadFiles;
    }

    public String getFrontendComponentKey() {
        return frontendComponentKey;
    }

    public String getRemoteAddress() {
        return remoteAddress;
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
