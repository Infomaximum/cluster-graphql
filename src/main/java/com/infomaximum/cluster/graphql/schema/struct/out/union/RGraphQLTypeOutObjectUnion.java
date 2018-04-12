package com.infomaximum.cluster.graphql.schema.struct.out.union;

import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;

import java.util.Collections;
import java.util.Set;

/**
 * Created by kris on 29.12.16.
 */
public class RGraphQLTypeOutObjectUnion extends RGraphQLType {

    private final Set<RGraphQLObjectTypeField> fields;

    public RGraphQLTypeOutObjectUnion(String name, String description, Set<RGraphQLObjectTypeField> fields) {
        super(name, description);
        this.fields = Collections.unmodifiableSet(fields);
    }

    public Set<RGraphQLObjectTypeField> getFields() {
        return fields;
    }

}
