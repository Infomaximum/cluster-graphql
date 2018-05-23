package com.infomaximum.cluster.graphql.struct;

import com.infomaximum.cluster.core.remote.struct.RemoteObject;

public interface ContextRequest extends RemoteObject {

    GRequest getRequest();
}
