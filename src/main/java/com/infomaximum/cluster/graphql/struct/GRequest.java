package com.infomaximum.cluster.graphql.struct;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.struct.Component;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.*;

/**
 * Created by kris on 23.01.17.
 */
public class GRequest implements RemoteObject {

    public static class UploadFiles {
        public String uploadFilesUUID;
        public List<String> names;
    }

    private final String frontendComponentKey;
    private final RemoteObject requestContext;

    private Set<String> externalNameVariables;
    private UploadFiles uploadFiles;

    public GRequest(
            String frontendComponentKey,
            RemoteObject requestContext,
            Set<String> externalNameVariables,
            UploadFiles uploadFiles
    ) {
        this.frontendComponentKey = frontendComponentKey;
        this.requestContext = requestContext;

        this.externalNameVariables = externalNameVariables;
        this.uploadFiles = uploadFiles;
    }

    public String getFrontendComponentKey() {
        return frontendComponentKey;
    }

    public RemoteObject getRequestContext() {
        return requestContext;
    }

    public Set<String> getExternalNameVariables() {
        return externalNameVariables;
    }

    public UploadFiles getUploadFiles() {
        return uploadFiles;
    }

    @Override
    public JSONObject serialize(Component component) {
        JSONObject out = new JSONObject();
        out.put("key", frontendComponentKey);

        JSONObject outContext = new JSONObject();
        outContext.put("type", requestContext.getClass().getName());
        outContext.put("value", requestContext.serialize(component));
        out.put("context", outContext);

        if (externalNameVariables != null && !externalNameVariables.isEmpty()) {
            JSONArray jVariables = new JSONArray();
            jVariables.addAll(externalNameVariables);
            out.put("variables", jVariables);
        }

        if (uploadFiles != null) {
            JSONObject outUploadFiles = new JSONObject();
            outUploadFiles.put("upload_files_uuid", uploadFiles.uploadFilesUUID);

            JSONArray outUploadFileNames = new JSONArray();
            outUploadFileNames.addAll(uploadFiles.names);
            outUploadFiles.put("names", outUploadFileNames);

            out.put("upload_files", outUploadFiles);
        }

        return out;
    }

    public static GRequest deserialize(Component component, Class classType, JSONObject json) throws ReflectiveOperationException {
        String frontendComponentKey = json.getAsString("key");

        JSONObject jsonContext = (JSONObject) json.get("context");
        Class classTypeContext = Class.forName(jsonContext.getAsString("type"));
        JSONObject jsonValue = (JSONObject) jsonContext.get("value");
        RemoteObject requestContext = RemoteObject.deserialize(component, classTypeContext, jsonValue);

        Set<String> externalVariables;
        if (json.containsKey("variables")) {
            externalVariables = new HashSet<String>();
            externalVariables.addAll((Collection<String>) json.get("variables"));
        } else {
            externalVariables = Collections.emptySet();
        }

        UploadFiles uploadFiles = null;
        if (json.containsKey("upload_files")) {
            JSONObject jUploadFiles = (JSONObject) json.get("upload_files");

            uploadFiles = new UploadFiles();
            uploadFiles.uploadFilesUUID = jUploadFiles.getAsString("upload_files_uuid");

            uploadFiles.names = new ArrayList<String>();
            for (Object oJUploadFileName : (JSONArray) jUploadFiles.get("names")) {
                uploadFiles.names.add((String) oJUploadFileName);
            }
        }

        return new GRequest(frontendComponentKey, requestContext, externalVariables, uploadFiles);
    }
}
