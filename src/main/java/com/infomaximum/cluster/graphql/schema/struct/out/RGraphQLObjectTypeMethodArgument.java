package com.infomaximum.cluster.graphql.schema.struct.out;

/**
 * Created by kris on 29.12.16.
 */
public class RGraphQLObjectTypeMethodArgument {

    public final String type;
    public final String name;
    public final String externalName;
    public final boolean isNotNull;

    public RGraphQLObjectTypeMethodArgument(String type, String name, String externalName, boolean isNotNull) {
        this.type = type;
        this.name = name;
        this.externalName = externalName;
        this.isNotNull=isNotNull;
    }
}
