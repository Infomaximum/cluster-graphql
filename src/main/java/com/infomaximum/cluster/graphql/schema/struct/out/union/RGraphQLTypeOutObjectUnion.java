package com.infomaximum.cluster.graphql.schema.struct.out.union;

import com.infomaximum.cluster.graphql.schema.struct.RGraphQLType;
import com.infomaximum.cluster.graphql.schema.struct.out.RGraphQLObjectTypeField;
import com.infomaximum.cluster.struct.Component;

import java.util.Collections;
import java.util.Set;

/**
 * Created by kris on 29.12.16.
 */
public class RGraphQLTypeOutObjectUnion extends RGraphQLType {

    private final Set<RGraphQLObjectTypeField> fields;

    public RGraphQLTypeOutObjectUnion(String name, Set<RGraphQLObjectTypeField> fields) {
        super(name);
        this.fields = Collections.unmodifiableSet(fields);
    }

    public Set<RGraphQLObjectTypeField> getFields() {
        return fields;
    }

//    @Override
//    public void serializeNative(Component component, JSONObject out) {
//        out.put("fields", RGraphQLType.serializeFields(component, fields));
//    }

}
