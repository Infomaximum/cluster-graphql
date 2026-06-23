package com.infomaximum.server.components.component1.graphql.out.query;

import com.infomaximum.cluster.graphql.anotation.GraphQLField;
import com.infomaximum.cluster.graphql.anotation.GraphQLTypeOutObject;

/**
 * Самоссылающийся тестовый тип для проверки лимита глубины запроса.
 * Поле {@code child} возвращает тот же тип, что позволяет строить запрос
 * произвольной глубины вложенности.
 */
@GraphQLTypeOutObject("recursive")
public class GRecursive {

    @GraphQLField
    public static int getValue() {
        return 0;
    }

    @GraphQLField
    public static Class<GRecursive> getChild() {
        return GRecursive.class;
    }
}
