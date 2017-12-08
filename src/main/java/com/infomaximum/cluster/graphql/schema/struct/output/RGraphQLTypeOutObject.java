package com.infomaximum.cluster.graphql.schema.struct.output;

import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.Collections;
import java.util.Set;

/**
 * Created by kris on 20.07.17.
 */
public class RGraphQLTypeOutObject extends RGraphQLType {

    private final String className;

    private final Set<String> unionGraphQLTypeNames;
    private final Set<RGraphQLObjectTypeField> fields;

    public RGraphQLTypeOutObject(String name, String className, Set<String> unionGraphQLTypeNames, Set<RGraphQLObjectTypeField> fields) {
        super(name);
        this.className = className;
        this.unionGraphQLTypeNames = Collections.unmodifiableSet(unionGraphQLTypeNames);
        this.fields = Collections.unmodifiableSet(fields);
    }

    public String getClassName() {
        return className;
    }

    public Set<String> getUnionGraphQLTypeNames() {
        return unionGraphQLTypeNames;
    }

    public Set<RGraphQLObjectTypeField> getFields() {
        return fields;
    }

    @Override
    public void serializeNative(JSONObject out) {
        out.put("class_name", className);

        JSONArray outUnions = new JSONArray();
        for (String unionGraphQLTypeName : unionGraphQLTypeNames) {
            outUnions.add(unionGraphQLTypeName);
        }
        out.put("unions", outUnions);

        out.put("fields", serializeFields(fields));
    }

    public static JSONArray serializeFields(Set<RGraphQLObjectTypeField> fields) {
        JSONArray outFields =new JSONArray();
        for (RGraphQLObjectTypeField field: fields) {
            JSONObject outField = new JSONObject();
            outField.put("subsystem", field.subsystem);
            outField.put("auth", field.typeAuthControl.name());
            outField.put("is_field", field.isField);
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
