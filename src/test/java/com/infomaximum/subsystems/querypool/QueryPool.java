package com.infomaximum.subsystems.querypool;

import com.infomaximum.subsystems.querypool.utils.LockGuard;
import com.infomaximum.utils.DefaultThreadFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class QueryPool {

    public enum LockType{
        SHARED, EXCLUSIVE
    }

    public enum Priority {
        LOW, HIGH
    }

    private static class QueryWrapper<T>{

        final Query<T> query;
        final CompletableFuture<T> future;

        final Map<Long, LockType> resources;

        QueryWrapper(Query<T> query) {
            this.query = query;
            this.future = new CompletableFuture<>();

            try (ResourceProviderImpl provider = new ResourceProviderImpl()) {
                query.prepare(provider);
                this.resources = provider.getResources();
            }
        }

        void execute() {
            try (QueryTransaction transaction = new QueryTransaction()) {
                T result = query.execute(transaction);
                query.onTransactionCommitted();
                future.complete(result);
            } catch (Exception ex) {
                query.onTransactionRollbacks();
                future.completeExceptionally(ex);
            }
        }
    }

    private static class QueryLockType {

        final QueryWrapper query;
        final LockType lockType;

        QueryLockType(QueryWrapper query, LockType lockType) {
            this.query = query;
            this.lockType = lockType;
        }
    }

    private static class ResourceMap extends HashMap<Long, ArrayList<QueryLockType>> { }

    public static final int MAX_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    public static final int MAX_WORKED_QUERY_COUNT = MAX_THREAD_COUNT * 5;
    public static final int MAX_WAITING_QUERY_COUNT = MAX_THREAD_COUNT * 20;

    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
            MAX_THREAD_COUNT,
            MAX_THREAD_COUNT,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(MAX_WORKED_QUERY_COUNT),
            new DefaultThreadFactory("QueryPool")
    );

    private final ReentrantLock lock = new ReentrantLock();
    private final ArrayList<String> maintenanceMarkers = new ArrayList<>();
    private final ResourceMap occupiedResources = new ResourceMap();
    private final ResourceMap waitingResources = new ResourceMap();

    private volatile int highPriorityWaitingQueryCount = 0;
    private volatile int lowPriorityWaitingQueryCount = 0;

    public <T> CompletableFuture<T> execute(Query<T> query) {
        QueryWrapper<T> queryWrapp = new QueryWrapper<T>(query);

        try (LockGuard guard = new LockGuard(lock)) {
            if (isOverloaded(queryWrapp.query.getPriority())) {
                queryWrapp.future.completeExceptionally(new RuntimeException());
            } else if (isOccupiedResources(queryWrapp.resources)) {
                if (isMaintenance()) {
                    queryWrapp.future.completeExceptionally(new RuntimeException());
                }
                else {
                    captureWaitingResources(queryWrapp);
                }
            } else {
                submitQuery(queryWrapp);
            }
        }

        return queryWrapp.future;
    }

    /**
     * @return null if query is not submitted
     */
    public <T> CompletableFuture<T> tryExecuteImmediately(Query<T> query) {
        QueryWrapper<T> queryWrapp = new QueryWrapper<T>(query);

        try (LockGuard guard = new LockGuard(lock)) {
            if (isOverloaded(queryWrapp.query.getPriority()) || isOccupiedResources(queryWrapp.resources)) {
                return null;
            }
            submitQuery(queryWrapp);
            return queryWrapp.future;
        }
    }

    public boolean waitingQueryExists(Priority priority) {
        switch (priority) {
            case LOW:
                return lowPriorityWaitingQueryCount != 0;
            case HIGH:
                return highPriorityWaitingQueryCount != 0;
        }
        return false;
    }

    public void shutdownAwait() throws InterruptedException {
        threadPool.shutdown();

        final HashSet<QueryWrapper> queries = new HashSet<>();
        try (LockGuard guard = new LockGuard(lock)) {
            waitingResources.forEach((key, value) -> value.forEach(item -> queries.add(item.query)));
            waitingResources.clear();
        }
        queries.forEach((query) -> query.future.completeExceptionally(new RuntimeException()));

        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public boolean isShutdown() {
        return threadPool.isShutdown();
    }

    public void await() throws InterruptedException {
        while (true) {
            try (LockGuard guard = new LockGuard(lock)) {
                if (waitingResources.isEmpty()) {
                    break;
                }
            }

            Thread.sleep(1000L);
        }

        shutdownAwait();
    }

    private void submitQuery(QueryWrapper<?> queryWrapp) {
        captureOccupiedResources(queryWrapp);

        try {
            threadPool.submit(() -> {
                try {
                    queryWrapp.execute();
                    try (LockGuard guard = new LockGuard(lock)) {
                        releaseOccupiedResources(queryWrapp);

                        //COMMENT Миронов В. можно оптимизировать поиск запросов на исполнение если releaseResources будет
                        // возвращать список ресурсов у которых нет активных Query или он был заблокирован на SHARED
                        trySubmitNextAvailableQueryBy(queryWrapp.resources);
                    }
                } catch (Throwable e) {
                    queryWrapp.future.completeExceptionally(e);
                    try (LockGuard guard = new LockGuard(lock)) {
                        releaseOccupiedResources(queryWrapp);
                    }
                }
            });
        } catch (RejectedExecutionException ignored) {
            releaseOccupiedResources(queryWrapp);
            queryWrapp.future.completeExceptionally(new RuntimeException());
        } catch (Throwable ex) {
            releaseOccupiedResources(queryWrapp);
            queryWrapp.future.completeExceptionally(ex);
        }
    }

    private void captureOccupiedResources(QueryWrapper queryWrapp){
        appendResources(queryWrapp, occupiedResources);
        pushMaintenance(queryWrapp.query.getMaintenanceMarker());
    }

    private void releaseOccupiedResources(QueryWrapper queryWrapp){
        popMaintenance(queryWrapp.query.getMaintenanceMarker());
        removeResources(queryWrapp, occupiedResources);
    }

    private void captureWaitingResources(QueryWrapper queryWrapp){
        switch (queryWrapp.query.getPriority()) {
            case LOW:
                ++lowPriorityWaitingQueryCount;
                break;
            case HIGH:
                ++highPriorityWaitingQueryCount;
                break;
        }
        appendResources(queryWrapp, waitingResources);
    }

    private void releaseWaitingResources(QueryWrapper queryWrapp){
        removeResources(queryWrapp, waitingResources);
        switch (queryWrapp.query.getPriority()) {
            case LOW:
                --lowPriorityWaitingQueryCount;
                break;
            case HIGH:
                --highPriorityWaitingQueryCount;
                break;
        }
    }

    private void trySubmitNextAvailableQueryBy(Map<Long, LockType> releasedResources) {
        HashSet<QueryWrapper> candidates = new HashSet<>();

        for (Map.Entry<Long, LockType> res : releasedResources.entrySet()) {
            ArrayList<QueryLockType> value = waitingResources.get(res.getKey());
            if (value != null) {
                value.forEach(item -> candidates.add(item.query));
            }
        }

        for (QueryWrapper<?> query : candidates) {
            if (isOccupiedResources(query.resources)) {
                continue;
            }

            if (isFilledThreadPool()) {
                break;
            }

            releaseWaitingResources(query);
            submitQuery(query);
        }
    }

    private boolean isOverloaded(Priority newQueryPriority) {
        if (isFilledThreadPool()) {
            return true;
        }

        switch (newQueryPriority) {
            case LOW:
                return lowPriorityWaitingQueryCount >= MAX_WAITING_QUERY_COUNT;
            case HIGH:
                return highPriorityWaitingQueryCount >= MAX_WAITING_QUERY_COUNT;
        }
        return false;
    }

    private boolean isFilledThreadPool() {
        return threadPool.getQueue().size() >= MAX_WORKED_QUERY_COUNT;
    }

    private void pushMaintenance(String marker) {
        if (marker != null) {
            maintenanceMarkers.add(marker);
        }
    }

    private void popMaintenance(String marker) {
        if (marker != null) {
            maintenanceMarkers.remove(maintenanceMarkers.size() - 1);
        }
    }

    private boolean isMaintenance() {
        return !maintenanceMarkers.isEmpty();
    }

    private boolean isOccupiedResources(final Map<Long, LockType> targetResources) {
        for (HashMap.Entry<Long, LockType> res : targetResources.entrySet()) {
            ArrayList<QueryLockType> foundValue = occupiedResources.get(res.getKey());
            if (foundValue == null || foundValue.isEmpty()) {
                continue;
            }

            if (res.getValue() == LockType.EXCLUSIVE || foundValue.get(0).lockType == LockType.EXCLUSIVE) {
                return true;
            }
        }
        return false;
    }

    private static void appendResources(QueryWrapper<?> query, ResourceMap destination) {
        for (Map.Entry<Long, LockType> entry: query.resources.entrySet()) {
            ArrayList<QueryLockType> foundValue = destination.get(entry.getKey());
            if (foundValue == null){
                foundValue = new ArrayList<>();
                destination.put(entry.getKey(), foundValue);
            }

            foundValue.add(new QueryLockType(query, entry.getValue()));
        }
    }

    private static void removeResources(QueryWrapper<?> query, ResourceMap destination) {
        for (Map.Entry<Long, LockType> entry: query.resources.entrySet()) {
            ArrayList<QueryLockType> foundValue = destination.get(entry.getKey());
            if (foundValue == null){
                continue;
            }

            foundValue.removeIf(item -> item.query == query);
            if (foundValue.isEmpty()) {
                destination.remove(entry.getKey());
            }
        }
    }
}
