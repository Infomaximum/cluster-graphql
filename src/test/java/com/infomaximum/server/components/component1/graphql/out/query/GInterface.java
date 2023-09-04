package com.infomaximum.server.components.component1.graphql.out.query;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;
import com.infomaximum.cluster.graphql.anotation.GraphQLField;
import com.infomaximum.cluster.graphql.anotation.GraphQLTypeOutObjectInterface;

@GraphQLTypeOutObjectInterface("interface")
public interface GInterface extends RemoteObject {

    @GraphQLField
    default String getDefaultMethod() {
        return "default_method";
    }

    @GraphQLField
    static String getStaticMethod() {
        return "static_method";
    }
}
