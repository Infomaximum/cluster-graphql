package com.infomaximum.server.components.component1.graphql.out.query;

import com.infomaximum.cluster.graphql.anotation.GraphQLTypeOutObject;

@GraphQLTypeOutObject("interface2")
public class GInterfaceImpl2 implements GInterface {

    @Override
    public String getDefaultMethod() {
        return "default_method2";
    }
}
