package com.infomaximum.subsystems.querypool;


public interface ResourceProvider extends com.infomaximum.cluster.querypool.ResourceProvider {

    void borrowResource(Class resClass, QueryPool.LockType type);
}
