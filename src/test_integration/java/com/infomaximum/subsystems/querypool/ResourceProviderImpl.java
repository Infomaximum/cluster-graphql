package com.infomaximum.subsystems.querypool;

import com.infomaximum.utils.CRC64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

class ResourceProviderImpl implements ResourceProvider, AutoCloseable {

    private final static Logger log = LoggerFactory.getLogger(ResourceProviderImpl.class);

    private final HashMap<Long, QueryPool.LockType> resources = new HashMap<>();
    private final CRC64 crc64 = new CRC64();
    private boolean closed = false;

    ResourceProviderImpl() {}

    @Override
    public void borrowResource(Class resClass, QueryPool.LockType type) {
        check();

        crc64.reset();
        crc64.update(resClass.getName().getBytes(StandardCharsets.UTF_8));
        resources.merge(crc64.getValue(), type, (val1, val2) -> val1 == QueryPool.LockType.EXCLUSIVE ? val1 : val2);
    }

    Map<Long, QueryPool.LockType> getResources() {
        check();

        return resources;
    }


    @Override
    public void close() {
        closed = true;
    }

    private void check() {
        if (closed) {
            throw new RuntimeException();
        }
    }

    @Override
    public Map<Long, Boolean> getLockResources() {
        HashMap<Long, Boolean> lockResources = new HashMap<>();
        for (Map.Entry<Long, QueryPool.LockType> entry: resources.entrySet()) {
            lockResources.put(entry.getKey(), (entry.getValue()==QueryPool.LockType.EXCLUSIVE));
        }
        return lockResources;
    }
}
