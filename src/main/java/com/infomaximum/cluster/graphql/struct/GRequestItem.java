package com.infomaximum.cluster.graphql.struct;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.struct.Component;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by kris on 23.01.17.
 */
public class GRequestItem implements RemoteObject {

    public final Object source;//Для какого объекта пришел запрос
    public final Set<String> receivedArguments;

    public GRequestItem(Object source, Set<String> receivedArguments) {
        this.source = source;
        this.receivedArguments = receivedArguments;
    }

    public Object getSource() {
        return source;
    }

    public Set<String> getReceivedArguments() {
        return receivedArguments;
    }

    @Override
    public JSONObject serialize(Component component) {
        JSONObject out = new JSONObject();

        if (source != null) {
            JSONObject outSource = new JSONObject();
            outSource.put("type", component.getRemotes().getRemotePackerObjects().getClassName(source.getClass()));
            outSource.put("value", component.getRemotes().getRemotePackerObjects().serialize(source));
            out.put("source", outSource);
        }

        if (receivedArguments != null && !receivedArguments.isEmpty()) {
            JSONArray outArguments = new JSONArray();
            for (String keyArguments : receivedArguments) {
                outArguments.add(keyArguments);
            }
            out.put("received_arguments", outArguments);
        }

        return out;
    }

    public static GRequestItem deserialize(Component component, Class classType, JSONObject json) throws ReflectiveOperationException {
        Object source = null;
        if (json.containsKey("source")) {
            JSONObject jSource = (JSONObject) json.get("source");

            String type = jSource.getAsString("type");
            Class classOfType = Class.forName(type, true, Thread.currentThread().getContextClassLoader());
            source = component.getRemotes().getRemotePackerObjects().deserialize(classOfType, jSource.get("value"));
        }

        Set<String> receivedArguments = new HashSet<String>();
        if (json.containsKey("received_arguments")) {
            JSONArray jArguments = (JSONArray) json.get("received_arguments");
            for (Object oItem : jArguments) {
                receivedArguments.add((String) oItem);
            }
        }

        return new GRequestItem(source, receivedArguments);
    }
}
