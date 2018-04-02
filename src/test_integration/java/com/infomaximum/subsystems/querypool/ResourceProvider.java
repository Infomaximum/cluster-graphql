package com.infomaximum.subsystems.querypool;


public interface ResourceProvider {

    void borrowResource(Class resClass, QueryPool.LockType type);
}
