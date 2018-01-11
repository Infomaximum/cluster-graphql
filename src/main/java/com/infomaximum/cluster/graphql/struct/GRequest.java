package com.infomaximum.cluster.graphql.struct;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.struct.Component;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Created by kris on 23.01.17.
 */
public class GRequest implements RemoteObject {

    private final String frontendComponentKey;
    private final RemoteObject requestContext;

    private Set<String> externalNameVariables;
    private Map<String, URI> uploadFiles;

    public GRequest(
            String frontendComponentKey,
            RemoteObject requestContext,
            Set<String> externalNameVariables,
            Map<String, URI> uploadFiles
    ) {
        this.frontendComponentKey = frontendComponentKey;
        this.requestContext = requestContext;

        this.externalNameVariables = externalNameVariables;
        this.uploadFiles = Collections.unmodifiableMap(uploadFiles);
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

    public Map<String, URI> getUploadFiles() {
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
            JSONArray outUploadFiles = new JSONArray();
            for (Map.Entry<String, URI> entry: uploadFiles.entrySet()){
                JSONObject outUploadFile = new JSONObject();
                outUploadFile.put("name", entry.getKey());
                outUploadFile.put("uri", entry.getValue().toString());
                outUploadFiles.add(outUploadFile);
            }
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

        Map<String, URI> uploadFiles = null;
        if (json.containsKey("upload_files")) {
            JSONArray jOUploadFiles = (JSONArray) json.get("upload_files");
            for (Object jOUploadFile: jOUploadFiles) {
                JSONObject jUploadFile = (JSONObject) jOUploadFile;
                try {
                    uploadFiles.put(
                            jUploadFile.getAsString("name"),
                            new URI(jUploadFile.getAsString("uri"))
                    );
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return new GRequest(frontendComponentKey, requestContext, externalVariables, uploadFiles);
    }
}
