package com.infomaximum.cluster.graphql.schema.struct.out;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;

import java.util.List;

/**
 * Created by kris on 29.12.16.
 */
public class RGraphQLObjectTypeField {

    public final String componentUuid;

    public final boolean isField;
    public final boolean isPrepare;

    public final String type;
    public final String name;
    public final String externalName;
    public final List<RGraphQLObjectTypeMethodArgument> arguments;
    public final RemoteObject configuration;
    public final String description;
    public final String deprecated;

    public RGraphQLObjectTypeField(String componentUuid, boolean isField, boolean isPrepare, String type, String name, String externalName, RemoteObject configuration, String description, String deprecated) {
        this(componentUuid, isField, isPrepare, type, name, externalName, null, configuration, description, deprecated);
    }

    public RGraphQLObjectTypeField(String componentUuid, boolean isField, boolean isPrepare, String type, String name, String externalName, List<RGraphQLObjectTypeMethodArgument> arguments, RemoteObject configuration, String description, String deprecated) {
        this.componentUuid = componentUuid;

        this.isField = isField;
        this.isPrepare = isPrepare;

        this.type = type;
        this.name = name;
        this.externalName = externalName;
        this.arguments = arguments;
        this.configuration = configuration;
        this.description = description;
        this.deprecated = deprecated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RGraphQLObjectTypeField field = (RGraphQLObjectTypeField) o;
        return name.equals(field.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
