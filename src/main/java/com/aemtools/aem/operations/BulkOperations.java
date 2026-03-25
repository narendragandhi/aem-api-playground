package com.aemtools.aem.operations;

import com.aemtools.aem.client.AemApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class BulkOperations {

    private static final Logger logger = LoggerFactory.getLogger(BulkOperations.class);
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final int DEFAULT_MAX_CONCURRENT = 5;

    private final AemApiClient client;
    private final ExecutorService executor;
    private int batchSize = DEFAULT_BATCH_SIZE;
    private int maxConcurrent = DEFAULT_MAX_CONCURRENT;

    public interface BulkOperation<T> {
        T execute() throws Exception;
    }

    public interface BulkResultHandler<T> {
        void onSuccess(T result, int index);
        void onError(Exception e, int index);
    }

    public BulkOperations(AemApiClient client) {
        this.client = client;
        this.executor = Executors.newFixedThreadPool(maxConcurrent);
    }

    public BulkOperations(AemApiClient client, int maxConcurrent, int batchSize) {
        this.client = client;
        this.maxConcurrent = maxConcurrent;
        this.batchSize = batchSize;
        this.executor = Executors.newFixedThreadPool(maxConcurrent);
    }

    public <T> List<BulkResult<T>> executeBulk(List<BulkOperation<T>> operations) {
        return executeBulk(operations, null);
    }

    public <T> List<BulkResult<T>> executeBulk(List<BulkOperation<T>> operations, BulkResultHandler<T> handler) {
        List<Future<BulkResult<T>>> futures = new ArrayList<>();
        List<BulkResult<T>> results = new ArrayList<>();

        for (int i = 0; i < operations.size(); i++) {
            final int index = i;
            final BulkOperation<T> operation = operations.get(i);
            
            Future<BulkResult<T>> future = executor.submit(() -> {
                try {
                    T result = operation.execute();
                    if (handler != null) {
                        handler.onSuccess(result, index);
                    }
                    return new BulkResult<>(index, result, null);
                } catch (Exception e) {
                    logger.error("Bulk operation {} failed", index, e);
                    if (handler != null) {
                        handler.onError(e, index);
                    }
                    return new BulkResult<>(index, null, e);
                }
            });
            futures.add(future);
        }

        for (Future<BulkResult<T>> future : futures) {
            try {
                results.add(future.get(60, TimeUnit.SECONDS));
            } catch (Exception e) {
                logger.error("Failed to get bulk operation result", e);
                results.add(new BulkResult<>(-1, null, e));
            }
        }

        return results;
    }

    public <T> void executeBulkAsync(List<BulkOperation<T>> operations, Consumer<List<BulkResult<T>>> callback) {
        executor.submit(() -> {
            try {
                List<BulkResult<T>> results = executeBulk(operations);
                callback.accept(results);
            } catch (Exception e) {
                logger.error("Async bulk operation failed", e);
            }
        });
    }

    public <T> List<BulkResult<T>> executeInBatches(List<BulkOperation<T>> operations) {
        List<BulkResult<T>> allResults = new ArrayList<>();
        
        for (int i = 0; i < operations.size(); i += batchSize) {
            int end = Math.min(i + batchSize, operations.size());
            List<BulkOperation<T>> batch = operations.subList(i, end);
            
            logger.info("Executing batch {}-{} of {}", i, end, operations.size());
            List<BulkResult<T>> batchResults = executeBulk(batch);
            allResults.addAll(batchResults);
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return allResults;
    }

    public BulkStatistics getStatistics(List<?> results) {
        long successCount = results.stream()
            .filter(r -> r instanceof BulkResult && ((BulkResult<?>) r).getError() == null)
            .count();
        long errorCount = results.size() - successCount;
        
        return new BulkStatistics(results.size(), successCount, errorCount);
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setMaxConcurrent(int maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class BulkResult<T> {
        private final int index;
        private final T result;
        private final Exception error;

        public BulkResult(int index, T result, Exception error) {
            this.index = index;
            this.result = result;
            this.error = error;
        }

        public int getIndex() {
            return index;
        }

        public T getResult() {
            return result;
        }

        public Exception getError() {
            return error;
        }

        public boolean isSuccess() {
            return error == null;
        }
    }

    public static class BulkStatistics {
        private final int total;
        private final long successCount;
        private final long errorCount;

        public BulkStatistics(int total, long successCount, long errorCount) {
            this.total = total;
            this.successCount = successCount;
            this.errorCount = errorCount;
        }

        public int getTotal() {
            return total;
        }

        public long getSuccessCount() {
            return successCount;
        }

        public long getErrorCount() {
            return errorCount;
        }

        public double getSuccessRate() {
            return total > 0 ? (double) successCount / total * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("BulkStatistics{total=%d, success=%d, errors=%d, successRate=%.2f%%}",
                total, successCount, errorCount, getSuccessRate());
        }
    }
}
