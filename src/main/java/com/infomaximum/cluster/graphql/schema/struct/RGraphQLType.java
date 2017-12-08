package com.infomaximum.cluster.graphql.schema.struct;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.anotation.TypeAuthControl;
import com.infomaximum.cluster.graphql.schema.struct.input.RGraphQLInputObjectTypeField;
import com.infomaximum.cluster.graphql.schema.struct.input.RGraphQLTypeInObject;
import com.infomaximum.cluster.graphql.schema.struct.output.RGraphQLObjectTypeField;
import com.infomaximum.cluster.graphql.schema.struct.output.RGraphQLObjectTypeMethodArgument;
import com.infomaximum.cluster.graphql.schema.struct.output.RGraphQLTypeOutObject;
import com.infomaximum.cluster.struct.Component;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by kris on 29.12.16.
 */
public abstract class RGraphQLType implements RemoteObject {

    private final String name;

    public RGraphQLType(String name) {
        this.name=name;
    }

    public String getName() {
        return name;
    }

    @Override
    public JSONObject serialize(Component component) {
        JSONObject out = new JSONObject();
        out.put("name", name);

        serializeNative(out);

        return out;
    }

    public abstract void serializeNative(JSONObject out);

    public static RGraphQLType deserialize(Component component, Class classType, JSONObject json) {
        String name = json.getAsString("name");

        RGraphQLType rGraphQLType;
        if (classType.isAssignableFrom(RGraphQLTypeEnum.class)) {
            Set<String> enumValues = new HashSet<String>();
            for (Object oEnumValue: (JSONArray)json.get("enum_values")) {
                enumValues.add((String) oEnumValue);
            }

            rGraphQLType = new RGraphQLTypeEnum(name, enumValues);
        } else if (classType.isAssignableFrom(RGraphQLTypeOutObject.class)) {
            String className = json.getAsString("class_name");

            Set<String> unionGraphQLTypeNames = new HashSet<>();
            for (Object oUnionGraphQLTypeName : (JSONArray) json.get("unions")) {
                unionGraphQLTypeNames.add((String) oUnionGraphQLTypeName);
            }

            Set<RGraphQLObjectTypeField> fields = deserializeField((JSONArray) json.get("fields"));

            rGraphQLType = new RGraphQLTypeOutObject(name, className, unionGraphQLTypeNames, fields);
        } else if (classType.isAssignableFrom(RGraphQLTypeInObject.class)) {
            Set<RGraphQLInputObjectTypeField> fields = new HashSet<RGraphQLInputObjectTypeField>();

            for (Object oField: (JSONArray)json.get("fields")) {
                JSONObject jField = (JSONObject) oField;

                RGraphQLInputObjectTypeField field = new RGraphQLInputObjectTypeField(
                        jField.getAsString("type"),
                        jField.getAsString("name"),
                        jField.getAsString("ext_name"),
                        (Boolean) jField.get("is_not_null")
                );
                fields.add(field);
            }

            rGraphQLType = new RGraphQLTypeInObject(name, fields);
        } else {
            throw new RuntimeException("Not support type: " + classType);
        }

        return rGraphQLType;
    }

    private static Set<RGraphQLObjectTypeField> deserializeField(JSONArray jFields) {
        Set<RGraphQLObjectTypeField> fields = new HashSet<>();
        for (Object oField : jFields) {
            JSONObject jField = (JSONObject) oField;

            List<RGraphQLObjectTypeMethodArgument> arguments = null;
            if (jField.containsKey("arguments")) {
                arguments = new ArrayList<RGraphQLObjectTypeMethodArgument>();
                for (Object oArgument : (JSONArray) jField.get("arguments")) {
                    JSONObject jArgumen = (JSONObject) oArgument;

                    RGraphQLObjectTypeMethodArgument argument = new RGraphQLObjectTypeMethodArgument(
                            jArgumen.getAsString("type"),
                            jArgumen.getAsString("name"),
                            jArgumen.getAsString("ext_name"),
                            (boolean) jArgumen.get("is_not_null")
                    );
                    arguments.add(argument);
                }
            }

            RGraphQLObjectTypeField field = new RGraphQLObjectTypeField(
                    jField.getAsString("subsystem"),
                    TypeAuthControl.valueOf(jField.getAsString("auth")),
                    (boolean) jField.get("is_field"),
                    jField.getAsString("type"),
                    jField.getAsString("name"),
                    jField.getAsString("ext_name"),
                    (String) jField.get("deprecated"),
                    arguments
            );
            fields.add(field);
        }

        return fields;
    }
}
