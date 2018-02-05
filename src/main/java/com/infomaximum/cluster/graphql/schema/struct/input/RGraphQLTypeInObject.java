package com.infomaximum.cluster.graphql.schema.struct.input;

import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.struct.Component;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.Collections;
import java.util.Set;

/**
 * Created by kris on 20.07.17.
 */
public class RGraphQLTypeInObject extends RGraphQLType {

    private final Set<RGraphQLInputObjectTypeField> fields;

    public RGraphQLTypeInObject(String name, Set<RGraphQLInputObjectTypeField> fields) {
        super(name);
        this.fields = Collections.unmodifiableSet(fields);
    }

    public Set<RGraphQLInputObjectTypeField> getFields() {
        return fields;
    }

    @Override
    public void serializeNative(Component component, JSONObject out) {
        JSONArray outFields =new JSONArray();
        for (RGraphQLInputObjectTypeField field: fields) {
            JSONObject outField = new JSONObject();
            outField.put("type", field.type);
            outField.put("name", field.name);
            outField.put("ext_name", field.externalName);
            outField.put("is_not_null", field.isNotNull);
            outFields.add(outField);
        }
        out.put("fields", outFields);
    }

}
