package com.infomaximum.cluster.graphql.schema.build;

import com.infomaximum.cluster.graphql.schema.struct.output.RGraphQLObjectTypeField;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MergeGraphQLTypeOutObject extends MergeGraphQLType {

    private Set<RGraphQLObjectTypeField> fields;

    public MergeGraphQLTypeOutObject(String name) {
        super(name);
        this.fields = new HashSet<>();
    }

    public void mergeFields(Set<RGraphQLObjectTypeField> rTypeGraphQLFields) {
        fields.addAll(rTypeGraphQLFields);
    }

    public Set<RGraphQLObjectTypeField> getFields() {
        return Collections.unmodifiableSet(fields);
    }
}
