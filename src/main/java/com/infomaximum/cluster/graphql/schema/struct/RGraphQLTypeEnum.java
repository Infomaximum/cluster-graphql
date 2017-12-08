package com.infomaximum.cluster.graphql.schema.struct;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.Collections;
import java.util.Set;

/**
 * Created by kris on 20.07.17.
 */
public class RGraphQLTypeEnum extends RGraphQLType {

    private final Set<String> enumValues;

    public RGraphQLTypeEnum(String name, Set<String> enumValues) {
        super(name);
        this.enumValues = Collections.unmodifiableSet(enumValues);;
    }

    public Set<String> getEnumValues() {
        return enumValues;
    }

    @Override
    public void serializeNative(JSONObject out) {
        JSONArray outEnumValues =new JSONArray();
        for (String enumValue: enumValues) {
            outEnumValues.add(enumValue);
        }
        out.put("enum_values", outEnumValues);
    }

}
