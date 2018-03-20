package com.infomaximum.cluster.graphql.prepare;

import com.infomaximum.cluster.graphql.customfield.PrepareCustomField;

import java.util.Set;

public class PrepareAndExecuteRequest {

    private Set<PrepareCustomField> prepareCustomFields;

    public PrepareAndExecuteRequest(Set<PrepareCustomField> prepareCustomFields) {
        this.prepareCustomFields = prepareCustomFields;
    }
}

