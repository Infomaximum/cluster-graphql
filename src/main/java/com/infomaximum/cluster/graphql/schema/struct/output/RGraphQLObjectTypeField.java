package com.infomaximum.cluster.graphql.schema.struct.output;

import com.infomaximum.cluster.graphql.anotation.TypeAuthControl;

import java.util.List;

/**
 * Created by kris on 29.12.16.
 */
public class RGraphQLObjectTypeField {

    public final String subsystem;

    public final boolean isField;
    public final String type;
    public final String name;
    public final String externalName;
    public final List<RGraphQLObjectTypeMethodArgument> arguments;
    public final TypeAuthControl typeAuthControl;
    public final String deprecated;

    public RGraphQLObjectTypeField(String subsystem, TypeAuthControl typeAuthControl, boolean isField, String type, String name, String externalName, String deprecated) {
        this(subsystem, typeAuthControl, isField, type, name, externalName, deprecated, null);
    }

    public RGraphQLObjectTypeField(String subsystem, TypeAuthControl typeAuthControl, boolean isField, String type, String name, String externalName, String deprecated, List<RGraphQLObjectTypeMethodArgument> arguments) {
        this.subsystem=subsystem;

        this.typeAuthControl=typeAuthControl;
        this.isField=isField;
        this.type = type;
        this.name = name;
        this.externalName = externalName;
        this.deprecated = (deprecated==null || deprecated.isEmpty())?null:deprecated;
        this.arguments=arguments;
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
