package com.infomaximum.cluster.graphql.schema.struct;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.schema.struct.in.RGraphQLInputObjectTypeField;
import com.infomaximum.cluster.graphql.schema.struct.in.RGraphQLTypeInObject;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeMethodArgument;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLTypeOutObject;
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

    protected static String FIELD_COMPONENT_UUID="component_uuid";
    protected static String FIELD_NAME="name";

    private final String name;

    public RGraphQLType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public JSONObject serialize(Component component) {
        JSONObject out = new JSONObject();
        out.put(FIELD_NAME, name);

        serializeNative(component, out);

        return out;
    }

    public abstract void serializeNative(Component component, JSONObject out);

    public static RGraphQLType deserialize(Component component, Class classType, JSONObject json) throws ReflectiveOperationException {
        String name = json.getAsString(FIELD_NAME);

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

            Set<RGraphQLObjectTypeField> fields = deserializeField(component, (JSONArray) json.get("fields"));

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

    private static Set<RGraphQLObjectTypeField> deserializeField(Component component, JSONArray jFields) throws ReflectiveOperationException{
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

            RemoteObject fieldConfiguration = null;
            if (jField.containsKey("configuration")) {
                JSONObject jFieldConfiguration = (JSONObject) jField.get("configuration");
                Class jFieldConfigurationClassType = Class.forName(jFieldConfiguration.getAsString("type"));
                JSONObject jFieldConfigurationValue = (JSONObject) jFieldConfiguration.get("value");
                fieldConfiguration = RemoteObject.deserialize(component, jFieldConfigurationClassType, jFieldConfigurationValue);
            }

            RGraphQLObjectTypeField field = new RGraphQLObjectTypeField(
                    jField.getAsString(FIELD_COMPONENT_UUID),
                    fieldConfiguration,
                    (boolean) jField.get("is_field"),
                    (boolean) jField.get(RGraphQLObjectTypeField.FIELD_QUERY_POOL),
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

    public static JSONArray serializeFields(Component component, Set<RGraphQLObjectTypeField> fields) {
        JSONArray outFields =new JSONArray();
        for (RGraphQLObjectTypeField field: fields) {
            JSONObject outField = new JSONObject();
            if (field.componentUuid!=null) {
                outField.put(FIELD_COMPONENT_UUID, field.componentUuid);
            }
            if (field.configuration!=null) {
                JSONObject outFieldConfiguration = new JSONObject();
                outFieldConfiguration.put("type", field.configuration.getClass().getName());
                outFieldConfiguration.put("value", field.configuration.serialize(component));
                outField.put("configuration", outFieldConfiguration);
            }
            outField.put("is_field", field.isField);
            outField.put(RGraphQLObjectTypeField.FIELD_QUERY_POOL, field.queryPool);
            outField.put("type", field.type);
            outField.put("name", field.name);
            outField.put("ext_name", field.externalName);
            if (field.deprecated!=null) outField.put("deprecated", field.deprecated);
            if (field.arguments!=null) {
                JSONArray outArguments =new JSONArray();
                for (RGraphQLObjectTypeMethodArgument argument: field.arguments) {
                    JSONObject outArgument = new JSONObject();
                    outArgument.put("type", argument.type);
                    outArgument.put("name", argument.name);
                    outArgument.put("ext_name", argument.externalName);
                    outArgument.put("is_not_null", argument.isNotNull);
                    outArguments.add(outArgument);
                }
                outField.put("arguments", outArguments);
            }
            outFields.add(outField);
        }
        return outFields;
    }
}
