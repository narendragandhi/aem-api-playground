package com.aemtools.aem.client;

import com.aemtools.aem.config.ConfigManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AemApiClient {

    private static final Logger logger = LoggerFactory.getLogger(AemApiClient.class);
    private static final long DEFAULT_CACHE_TTL_MS = 300000;
    private static final int MAX_CACHE_SIZE = 500;
    
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConfigManager configManager;
    private boolean debugMode;
    private boolean enforceHttps = false;
    private boolean cacheEnabled = true;
    private final Map<String, String> auditLog = new ConcurrentHashMap<>();
    
    private final Map<String, CacheEntry> responseCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private long cacheTtlMs = DEFAULT_CACHE_TTL_MS;

    public AemApiClient() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(10);
        cm.setDefaultMaxPerRoute(5);
        this.httpClient = HttpClients.custom()
            .setConnectionManager(cm)
            .build();
        this.objectMapper = new ObjectMapper();
        this.configManager = ConfigManager.getInstance();
        this.debugMode = configManager.isDebugEnabled();
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public void setCacheEnabled(boolean enabled) {
        this.cacheEnabled = enabled;
    }

    public void setCacheTtlMs(long ttlMs) {
        this.cacheTtlMs = ttlMs;
    }

    public void clearCache() {
        responseCache.clear();
        logger.info("API response cache cleared");
    }

    public Map<String, Object> getCacheStats() {
        long now = System.currentTimeMillis();
        int validEntries = 0;
        for (CacheEntry entry : responseCache.values()) {
            if (now - entry.timestamp < cacheTtlMs) {
                validEntries++;
            }
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("enabled", cacheEnabled);
        stats.put("total_entries", responseCache.size());
        stats.put("valid_entries", validEntries);
        stats.put("ttl_seconds", cacheTtlMs / 1000);
        stats.put("max_entries", MAX_CACHE_SIZE);
        return stats;
    }

    public void setEnforceHttps(boolean enforceHttps) {
        this.enforceHttps = enforceHttps;
    }

    private static class CacheEntry {
        final String response;
        final long timestamp;
        
        CacheEntry(String response) {
            this.response = response;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isValid() {
            return System.currentTimeMillis() - timestamp < DEFAULT_CACHE_TTL_MS;
        }
    }

    private String generateCacheKey(String method, String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((method + ":" + url).getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return (method + ":" + url).hashCode() + "";
        }
    }

    public JsonNode get(String path) throws IOException {
        String url = buildUrl(path);
        String cacheKey = generateCacheKey("GET", url);
        
        if (cacheEnabled) {
            CacheEntry entry = responseCache.get(cacheKey);
            if (entry != null && entry.isValid()) {
                if (debugMode) {
                    logger.info("[CACHE HIT] GET {}", url);
                }
                return objectMapper.readTree(entry.response);
            }
        }
        
        JsonNode result = execute(new HttpGet(url));
        
        if (cacheEnabled) {
            responseCache.put(cacheKey, new CacheEntry(result.toString()));
            if (debugMode) {
                logger.info("[CACHED] GET {}", url);
            }
        }
        
        return result;
    }

    public JsonNode post(String path, Object body) throws IOException {
        HttpPost request = new HttpPost(buildUrl(path));
        String json = objectMapper.writeValueAsString(body);
        request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        return execute(request);
    }

    public JsonNode put(String path, Object body) throws IOException {
        HttpPut request = new HttpPut(buildUrl(path));
        String json = objectMapper.writeValueAsString(body);
        request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        return execute(request);
    }

    public boolean delete(String path) throws IOException {
        HttpDelete request = new HttpDelete(buildUrl(path));
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            logAudit("DELETE", path, response.getCode());
            return response.getCode() >= 200 && response.getCode() < 300;
        }
    }

    public byte[] download(String path) throws IOException {
        HttpGet request = new HttpGet(buildUrl(path));
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            logAudit("DOWNLOAD", path, response.getCode());
            return response.getEntity().getContent().readAllBytes();
        }
    }

    public JsonNode upload(String path, byte[] data, String contentType) throws IOException {
        HttpPost request = new HttpPost(buildUrl(path));
        request.setEntity(new StringEntity(new String(data), ContentType.create(contentType)));
        return execute(request);
    }

    public JsonNode move(String sourcePath, String destPath) throws IOException {
        BasicClassicHttpRequest request = new BasicClassicHttpRequest("MOVE", buildUrl(sourcePath));
        request.setHeader("X-Destination", destPath);
        request.setHeader("X-Overwrite", "T");
        request.setHeader("X-Depth", "infinity");
        return executeDirect(request);
    }

    public JsonNode copy(String sourcePath, String destPath) throws IOException {
        BasicClassicHttpRequest request = new BasicClassicHttpRequest("COPY", buildUrl(sourcePath));
        request.setHeader("X-Destination", destPath);
        request.setHeader("X-Overwrite", "T");
        request.setHeader("X-Depth", "infinity");
        return executeDirect(request);
    }

    private JsonNode executeDirect(BasicClassicHttpRequest request) throws IOException {
        String token = configManager.getActiveAccessToken();
        String basicAuth = configManager.getActiveBasicAuth();
        
        if (basicAuth != null && !basicAuth.isEmpty()) {
            request.setHeader("Authorization", "Basic " + basicAuth);
        } else if (token != null && !token.isEmpty()) {
            request.setHeader("Authorization", "Bearer " + token);
        }
        request.setHeader("Accept", "application/json");

        String path;
        String method = request.getMethod();

        try {
            path = request.getUri().toString();
        } catch (Exception e) {
            path = "(URI unavailable)";
        }

        if (debugMode) {
            try {
                logger.info("Request: {} {}", method, request.getUri());
            } catch (Exception e) {
                logger.info("Request: {} (URI unavailable)", method);
            }
        }

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody;
            try {
                responseBody = EntityUtils.toString(response.getEntity());
            } catch (Exception e) {
                responseBody = "";
            }
            
            int statusCode = response.getCode();
            logAudit(method, path, statusCode);

            if (debugMode) {
                logger.info("Response: {} - {}", statusCode, responseBody.substring(0, Math.min(200, responseBody.length())));
            }

            if (statusCode >= 200 && statusCode < 300) {
                if (responseBody.isEmpty()) {
                    return objectMapper.createObjectNode();
                }
                return objectMapper.readTree(responseBody);
            } else if (statusCode == 404) {
                throw new IOException("Not found: " + path);
            } else if (statusCode == 401) {
                throw new IOException("Unauthorized: " + path);
            } else if (statusCode == 409) {
                throw new IOException("Conflict: " + path);
            } else {
                throw new IOException("HTTP " + statusCode + ": " + responseBody);
            }
        }
    }

    private JsonNode execute(HttpUriRequestBase request) throws IOException {
        String token = configManager.getActiveAccessToken();
        String basicAuth = configManager.getActiveBasicAuth();
        
        if (basicAuth != null && !basicAuth.isEmpty()) {
            request.setHeader("Authorization", "Basic " + basicAuth);
        } else if (token != null && !token.isEmpty()) {
            request.setHeader("Authorization", "Bearer " + token);
        }
        request.setHeader("Accept", "application/json");

        String path;
        String method = request.getMethod();

        try {
            path = request.getUri().toString();
        } catch (Exception e) {
            path = "(URI unavailable)";
        }

        if (debugMode) {
            try {
                logger.info("Request: {} {}", method, request.getUri());
            } catch (Exception e) {
                logger.info("Request: {} (URI unavailable)", method);
            }
        }

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody;
            try {
                responseBody = EntityUtils.toString(response.getEntity());
            } catch (Exception e) {
                responseBody = "";
            }
            
            int statusCode = response.getCode();
            logAudit(method, path, statusCode);

            if (debugMode) {
                logger.info("Response: {} - {}", statusCode, responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);
            }

            if (statusCode >= 200 && statusCode < 300) {
                if (responseBody == null || responseBody.isEmpty()) {
                    return objectMapper.createObjectNode();
                }
                return objectMapper.readTree(responseBody);
            } else {
                throw new IOException("HTTP " + statusCode + ": " + responseBody);
            }
        }
    }

    private void logAudit(String method, String path, int statusCode) {
        String key = Instant.now().toString() + "|" + method + "|" + path + "|" + statusCode;
        auditLog.put(key, method + " " + path + " -> " + statusCode);
        logger.info("AUDIT: {} {} -> {}", method, path, statusCode);
    }

    public Map<String, String> getAuditLog() {
        return new ConcurrentHashMap<>(auditLog);
    }

    private String buildUrl(String path) {
        String baseUrl = configManager.getActiveEnvironmentUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalStateException("No active environment URL configured");
        }
        
        if (enforceHttps && baseUrl.startsWith("http://")) {
            throw new IllegalStateException("HTTPS enforcement is enabled but URL uses HTTP: " + baseUrl);
        }
        
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            baseUrl += "/";
        } else if (baseUrl.endsWith("/") && path.startsWith("/")) {
            path = path.substring(1);
        }
        
        return baseUrl + path;
    }

    public void close() throws IOException {
        httpClient.close();
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public String getAccessToken() {
        return configManager.getActiveAccessToken();
    }

    public String getBaseUrl() {
        return configManager.getActiveEnvironmentUrl();
    }
}
