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
    public final String deprecated;

    public RGraphQLObjectTypeField(String componentUuid, RemoteObject configuration, boolean isPrepare, boolean isField, String type, String name, String externalName, String deprecated) {
        this(componentUuid, configuration, isPrepare, isField, type, name, externalName, deprecated, null);
    }

    public RGraphQLObjectTypeField(String componentUuid, RemoteObject configuration, boolean isPrepare, boolean isField, String type, String name, String externalName, String deprecated, List<RGraphQLObjectTypeMethodArgument> arguments) {
        this.componentUuid = componentUuid;

        this.isField = isField;
        this.isPrepare = isPrepare;

        this.type = type;
        this.name = name;
        this.externalName = externalName;
        this.arguments = arguments;
        this.configuration = configuration;
        this.deprecated = (deprecated==null || deprecated.isEmpty())?null:deprecated;
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
