package com.infomaximum.cluster.graphql.schema.build;

import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;

import java.util.*;

public class MergeGraphQLTypeOutObjectUnion extends MergeGraphQLType {

    /**
     * key - class name
     * value - graphql name
     */
    private final Map<String, String> possibleType;

    private Set<RGraphQLObjectTypeField> fields;

    public MergeGraphQLTypeOutObjectUnion(String name, String description) {
        super(name, description);
        this.possibleType = new HashMap<>();
        this.fields = new HashSet<>();
    }

    public void mergePossible(String className, String graphQLTypeName) {
        possibleType.put(className, graphQLTypeName);
    }

    public void mergeFields(Set<RGraphQLObjectTypeField> rTypeGraphQLFields) {
        fields.addAll(rTypeGraphQLFields);
    }

    public Collection<String> getPossibleTypeNames() {
        return possibleType.values();
    }

    public Set<RGraphQLObjectTypeField> getFields() {
        return Collections.unmodifiableSet(fields);
    }

    public String getGraphQLTypeName(String className) {
        return possibleType.get(className);
    }
}
