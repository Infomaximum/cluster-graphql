package com.infomaximum.cluster.graphql.schema;

/**
 * Created by kris on 26.12.16.
 */
public enum TypeSchema {

    QUERY("query"),
    MUTATION("mutation");

    private final String value;

    private TypeSchema(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
