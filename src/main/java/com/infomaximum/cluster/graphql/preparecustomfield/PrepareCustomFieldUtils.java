package com.infomaximum.cluster.graphql.preparecustomfield;

import com.infomaximum.cluster.graphql.struct.GRequest;
import graphql.language.Field;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;

public class PrepareCustomFieldUtils {

    public static String uniqueFieldKey(GRequest request, DataFetchingEnvironment dataFetchingEnvironment) {
        if (dataFetchingEnvironment.getFields().size() != 1) {
            throw new RuntimeException("Not support mode");
        }
        Field field = dataFetchingEnvironment.getFields().get(0);

        return uniqueFieldKey(request, field);
    }

    public static String uniqueFieldKey(GRequest request, Field field) {
        SourceLocation sourceLocation = field.getSourceLocation();

        return new StringBuilder()
                .append(request.getUuid()).append(':')
                .append(sourceLocation.getLine()).append(':')
                .append(sourceLocation.getColumn())
                .toString();
    }


}
