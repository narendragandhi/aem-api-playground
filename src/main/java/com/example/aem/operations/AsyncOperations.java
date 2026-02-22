package com.example.aem.operations;

import com.example.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class AsyncOperations {

    private static final Logger logger = LoggerFactory.getLogger(AsyncOperations.class);
    private static final int DEFAULT_POLL_INTERVAL_MS = 2000;
    private static final int DEFAULT_TIMEOUT_MS = 300000;

    private final AemApiClient client;
    private final ExecutorService executor;
    private int pollIntervalMs = DEFAULT_POLL_INTERVAL_MS;
    private int timeoutMs = DEFAULT_TIMEOUT_MS;

    public interface AsyncCallback<T> {
        void onComplete(T result);
        void onError(Exception e);
        void onProgress(String status);
    }

    public interface PollingCondition {
        boolean isComplete(JsonNode response);
    }

    public AsyncOperations(AemApiClient client) {
        this.client = client;
        this.executor = Executors.newCachedThreadPool();
    }

    public <T> void executeAsync(Callable<T> task, AsyncCallback<T> callback) {
        executor.submit(() -> {
            try {
                T result = task.call();
                callback.onComplete(result);
            } catch (Exception e) {
                logger.error("Async operation failed", e);
                callback.onError(e);
            }
            return null;
        });
    }

    public JsonNode executeAndPoll(String path, PollingCondition condition) throws IOException {
        return executeAndPoll(path, condition, null);
    }

    public JsonNode executeAndPoll(String path, PollingCondition condition, Consumer<String> progressCallback) throws IOException {
        JsonNode initialResponse = client.post(path, Map.of());
        
        if (condition.isComplete(initialResponse)) {
            return initialResponse;
        }

        String statusUrl = initialResponse.has("statusUrl") ? 
            initialResponse.get("statusUrl").asText() : null;
        
        if (statusUrl == null) {
            throw new IOException("No status URL found in response");
        }

        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Polling interrupted");
            }

            JsonNode statusResponse = client.get(statusUrl);
            
            if (progressCallback != null) {
                String status = statusResponse.has("status") ? 
                    statusResponse.get("status").asText() : "unknown";
                progressCallback.accept(status);
            }

            if (condition.isComplete(statusResponse)) {
                return statusResponse;
            }

            if (statusResponse.has("error")) {
                throw new IOException("Async operation failed: " + statusResponse.get("error").asText());
            }
        }

        throw new IOException("Async operation timed out after " + timeoutMs + "ms");
    }

    public <T> CompletableFuture<T> toCompletableFuture(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    public void executeWithTimeout(Runnable task, int timeoutSeconds) {
        executor.submit(() -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("Task execution failed", e);
            }
        });
    }

    public <T> T getWithTimeout(Callable<T> task, int timeoutSeconds) throws Exception {
        Future<T> future = executor.submit(task);
        return future.get(timeoutSeconds, TimeUnit.SECONDS);
    }

    public void setPollIntervalMs(int pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
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
}
