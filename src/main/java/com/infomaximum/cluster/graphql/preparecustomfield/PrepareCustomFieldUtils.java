package com.infomaximum.cluster.graphql.preparecustomfield;

import com.infomaximum.cluster.graphql.exception.GraphQLExecutorInvalidSyntaxException;
import graphql.language.Field;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;

public class PrepareCustomFieldUtils {

    public static String getKeyField(DataFetchingEnvironment dataFetchingEnvironment) {
        if (dataFetchingEnvironment.getFields().size() < 1) {
            throw new RuntimeException("Not support zero field");
        } else if (dataFetchingEnvironment.getFields().size() > 1) {
            String fieldName = dataFetchingEnvironment.getFields().get(0).getName();
            throw new GraphQLExecutorInvalidSyntaxException("Multi fields: " + fieldName);
        }
        Field field = dataFetchingEnvironment.getFields().get(0);

        return getKeyField(field);
    }

    public static String getKeyField(Field field) {
        SourceLocation sourceLocation = field.getSourceLocation();

        return new StringBuilder()
                .append(sourceLocation.getLine()).append(':')
                .append(sourceLocation.getColumn())
                .toString();
    }


}
