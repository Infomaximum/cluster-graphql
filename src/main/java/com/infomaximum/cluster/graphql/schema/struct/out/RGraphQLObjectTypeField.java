package com.infomaximum.cluster.graphql.schema.struct.out;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;

import java.util.List;

/**
 * Created by kris on 29.12.16.
 */
public class RGraphQLObjectTypeField {

    public final Integer componentUniqueId;

    public final boolean isField;
    public final boolean isPrepare;

    public final String type;
    public final String name;
    public final String externalName;
    public final List<RGraphQLObjectTypeMethodArgument> arguments;
    public final RemoteObject configuration;
    public final String description;
    public final String deprecated;

    public RGraphQLObjectTypeField(Integer componentUniqueId, boolean isField, boolean isPrepare, String type, String name, String externalName, RemoteObject configuration, String description, String deprecated) {
        this(componentUniqueId, isField, isPrepare, type, name, externalName, null, configuration, description, deprecated);
    }

    public RGraphQLObjectTypeField(Integer componentUniqueId, boolean isField, boolean isPrepare, String type, String name, String externalName, List<RGraphQLObjectTypeMethodArgument> arguments, RemoteObject configuration, String description, String deprecated) {
        this.componentUniqueId = componentUniqueId;

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
