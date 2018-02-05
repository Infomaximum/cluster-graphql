package com.infomaximum.cluster.graphql.schema.struct.output;

import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.struct.Component;
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
    public void serializeNative(Component component, JSONObject out) {
        out.put("class_name", className);

        JSONArray outUnions = new JSONArray();
        for (String unionGraphQLTypeName : unionGraphQLTypeNames) {
            outUnions.add(unionGraphQLTypeName);
        }
        out.put("unions", outUnions);

        out.put("fields", serializeFields(component, fields));
    }
}
