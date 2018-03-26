package com.infomaximum.cluster.graphql.schema.build;

import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;

import java.util.*;

public class MergeGraphQLTypeOutObject extends MergeGraphQLType {

    private Map<String, RGraphQLObjectTypeField> fieldsByExternalName;

    public MergeGraphQLTypeOutObject(String name) {
        super(name);
        this.fieldsByExternalName = new HashMap<>();
    }

    public void mergeFields(Set<RGraphQLObjectTypeField> rTypeGraphQLFields) {
        for (RGraphQLObjectTypeField field: rTypeGraphQLFields) {
            fieldsByExternalName.put(field.externalName, field);
        }
    }

    public Collection<RGraphQLObjectTypeField> getFields() {
        return fieldsByExternalName.values();
    }

    public RGraphQLObjectTypeField getFieldByExternalName(String externalName) {
        return fieldsByExternalName.get(externalName);
    }
}
