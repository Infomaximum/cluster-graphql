package com.infomaximum.cluster.graphql.schema;

import com.infomaximum.cluster.graphql.exception.GraphQLExecutorException;
import com.infomaximum.cluster.graphql.fieldargument.custom.CustomFieldArgument;
import com.infomaximum.cluster.graphql.preparecustomfield.PrepareCustomField;
import com.infomaximum.cluster.graphql.schema.scalartype.GraphQLTypeScalar;
import com.infomaximum.cluster.graphql.utils.CaseFormat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

public class GraphQLSchemaType {

    public final Set<GraphQLTypeScalar> typeScalars;
    private final HashMap<Class, GraphQLTypeScalar> hashTypeScalarByClass;

    public final Set<PrepareCustomField> prepareCustomFields;

    public final Set<CustomFieldArgument> customArguments;

    public GraphQLSchemaType(
            Set<GraphQLTypeScalar> scalarTypes,
            Set<PrepareCustomField> prepareCustomFields,
            Set<CustomFieldArgument> customArguments
    ) throws GraphQLExecutorException {
        this.typeScalars = Collections.unmodifiableSet(scalarTypes);
        this.hashTypeScalarByClass = new HashMap<>();
        for (GraphQLTypeScalar typeScalar : typeScalars) {
            for (Class clazz : typeScalar.getAssociationClasses()) {
                if (hashTypeScalarByClass.putIfAbsent(clazz, typeScalar) != null) {
                    throw new GraphQLExecutorException("Conflict scalar type class: " + clazz);
                }
            }
        }

        this.prepareCustomFields = Collections.unmodifiableSet(prepareCustomFields);

        this.customArguments = Collections.unmodifiableSet(customArguments);
    }

    public GraphQLTypeScalar getTypeScalarByClass(Class clazz) {
        return hashTypeScalarByClass.get(clazz);
    }

    public static String convertToGraphQLName(String name) {
        return CaseFormat.toLowerUnderscore(name);
    }

}
