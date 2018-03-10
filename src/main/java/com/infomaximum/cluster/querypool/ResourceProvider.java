package com.infomaximum.cluster.querypool;

import java.util.Map;

public interface ResourceProvider {

    /**
     * Возврощаем залоченные ресурсы
     * key: ресурс
     * value: true - экслюзивная блокировка
     * @return
     */
    public Map<Long, Boolean> getLockResources();
}
