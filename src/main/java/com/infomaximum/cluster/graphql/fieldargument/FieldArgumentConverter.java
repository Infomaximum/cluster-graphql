package com.infomaximum.cluster.graphql.fieldargument;

import com.infomaximum.cluster.graphql.fieldargument.custom.CustomFieldArgument;
import graphql.schema.GraphQLScalarType;

import java.util.Collections;
import java.util.Set;

public class FieldArgumentConverter {

    public final Set<GraphQLScalarType> scalarTypes;

    public final Set<CustomFieldArgument> customArguments;

    public FieldArgumentConverter(Set<GraphQLScalarType> scalarTypes, Set<CustomFieldArgument> customArguments) {
        this.scalarTypes = Collections.unmodifiableSet(scalarTypes);
        this.customArguments = Collections.unmodifiableSet(customArguments);
    }
}
