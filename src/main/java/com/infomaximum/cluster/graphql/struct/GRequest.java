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

    public static class UploadFile {
        public final String field;
        public final String filename;
        public final URI uri;

        public UploadFile(String field, String filename, URI uri) {
            this.field = field;
            this.filename = filename;
            this.uri = uri;
        }
    }

    private final String frontendComponentKey;
    private final RemoteObject requestContext;

    private Set<String> externalNameVariables;
    private List<UploadFile> uploadFiles;

    public GRequest(
            String frontendComponentKey,
            RemoteObject requestContext,
            Set<String> externalNameVariables,
            List<UploadFile> uploadFiles
    ) {
        this.frontendComponentKey = frontendComponentKey;
        this.requestContext = requestContext;

        this.externalNameVariables = externalNameVariables;
        this.uploadFiles = (uploadFiles==null)?null:Collections.unmodifiableList(uploadFiles);
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

    public List<UploadFile> getUploadFiles() {
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
            for (UploadFile uploadFile: uploadFiles){
                JSONObject outUploadFile = new JSONObject();
                outUploadFile.put("field", uploadFile.field);
                outUploadFile.put("filename", uploadFile.filename);
                outUploadFile.put("uri", uploadFile.uri.toString());
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

        List<UploadFile> uploadFiles = null;
        if (json.containsKey("upload_files")) {
            JSONArray jOUploadFiles = (JSONArray) json.get("upload_files");
            for (Object jOUploadFile: jOUploadFiles) {
                JSONObject jUploadFile = (JSONObject) jOUploadFile;
                try {
                    uploadFiles.add(
                            new UploadFile(
                                    jUploadFile.getAsString("field"),
                                    jUploadFile.getAsString("filename"),
                                    new URI(jUploadFile.getAsString("uri"))
                            )
                    );
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return new GRequest(frontendComponentKey, requestContext, externalVariables, uploadFiles);
    }
}
