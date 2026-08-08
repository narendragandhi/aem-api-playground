package com.aemtools.aem.client;

import com.aemtools.aem.audit.AuditLogger;
import com.aemtools.aem.config.ConfigManager;
import com.aemtools.aem.config.LoggerManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
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
    private String httpProxy;
    private String httpsProxy;
    private String noProxy;
    private String baseUrlOverride;
    private String authOverride;
    
    private final Map<String, CacheEntry> responseCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };
    private long cacheTtlMs = DEFAULT_CACHE_TTL_MS;

    public AemApiClient() {
        this(null, null, null);
    }

    public AemApiClient(String httpProxy, String httpsProxy, String noProxy) {
        this.httpProxy = httpProxy;
        this.httpsProxy = httpsProxy;
        this.noProxy = noProxy;
        
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(10);
        cm.setDefaultMaxPerRoute(5);
        
        var builder = HttpClients.custom()
            .setConnectionManager(cm);
        
        if (httpProxy != null && !httpProxy.isEmpty()) {
            logger.info("Proxy configured: http={}, https={}, no-proxy={}", httpProxy, httpsProxy, noProxy);
        }
        
        this.httpClient = builder.build();
        this.objectMapper = new ObjectMapper();
        this.configManager = ConfigManager.getInstance();
        this.debugMode = configManager.isDebugEnabled();
    }

    public void setProxy(String httpProxy, String httpsProxy, String noProxy) {
        this.httpProxy = httpProxy;
        this.httpsProxy = httpsProxy;
        this.noProxy = noProxy;
        logger.info("Proxy settings updated: http={}, https={}, no-proxy={}", httpProxy, httpsProxy, noProxy);
    }

    /**
     * Points this client at an explicit base URL and auth header, overriding the
     * globally active environment. Used by recipes that operate across two AEM
     * environments (e.g. package migration) where the destination is not the
     * active environment.
     *
     * @param baseUrl     AEM base URL (e.g. https://author.example.com)
     * @param authHeader  full Authorization header value (e.g. "Basic <b64>" or "Bearer <token>")
     * @return this client for chaining
     */
    public AemApiClient withTarget(String baseUrl, String authHeader) {
        this.baseUrlOverride = baseUrl;
        this.authOverride = authHeader;
        return this;
    }

    private boolean shouldBypassProxy(String host) {
        if (noProxy == null || noProxy.isEmpty()) {
            return false;
        }
        for (String noProxyHost : noProxy.split(",")) {
            String trim = noProxyHost.trim();
            if (host.equals(trim) || host.endsWith("." + trim) || "localhost".equals(trim) && host.startsWith("localhost")) {
                return true;
            }
        }
        return false;
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
            byte[] hash = md.digest((method + ":" + url).getBytes(StandardCharsets.UTF_8));
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
        applyAuth(request);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            logAudit("DELETE", path, response.getCode());
            return response.getCode() >= 200 && response.getCode() < 300;
        }
    }

    public byte[] download(String path) throws IOException {
        HttpGet request = new HttpGet(buildUrl(path));
        applyAuth(request);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            logAudit("DOWNLOAD", path, response.getCode());
            int code = response.getCode();
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + " while downloading " + path);
            }
            return response.getEntity().getContent().readAllBytes();
        }
    }

    public JsonNode upload(String path, byte[] data, String contentType) throws IOException {
        HttpPost request = new HttpPost(buildUrl(path));
        request.setEntity(new ByteArrayEntity(data, ContentType.create(contentType)));
        return execute(request);
    }

    /**
     * A raw HTTP response carrying the status code and the body as a string.
     * Used for endpoints that return XML or other non-JSON payloads.
     */
    public record RawResponse(int statusCode, String body) {
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    /**
     * Executes a POST and returns the raw response body without JSON parsing.
     * The PackMgr service.jsp endpoints return XML and must not be parsed as JSON.
     */
    public RawResponse postRaw(String path) throws IOException {
        HttpPost request = new HttpPost(buildUrl(path));
        applyAuth(request);
        request.setHeader("Accept", "text/xml, application/xml, application/json, */*");
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String body = readBody(response);
            logAudit("POST", path, response.getCode());
            return new RawResponse(response.getCode(), body);
        }
    }

    /**
     * Uploads a file using a multipart/form-data body. The PackMgr upload
     * endpoint expects a part named {@code file}, not a raw byte body.
     *
     * @return the raw response body (XML for PackMgr)
     */
    public String uploadMultipart(String path, String fieldName, String fileName, byte[] data) throws IOException {
        String boundary = "------------------------" + Long.toHexString(System.nanoTime());
        HttpPost request = new HttpPost(buildUrl(path));
        applyAuth(request);

        String prefix = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n"
            + "Content-Type: application/zip\r\n\r\n";
        String suffix = "\r\n--" + boundary + "--\r\n";
        byte[] head = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] tail = suffix.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[head.length + data.length + tail.length];
        System.arraycopy(head, 0, body, 0, head.length);
        System.arraycopy(data, 0, body, head.length, data.length);
        System.arraycopy(tail, 0, body, head.length + data.length, tail.length);

        request.setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
        request.setEntity(new ByteArrayEntity(body, null));
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody = readBody(response);
            logAudit("UPLOAD", path, response.getCode());
            return responseBody;
        }
    }

    private String readBody(CloseableHttpResponse response) throws IOException {
        if (response.getEntity() == null) {
            return "";
        }
        try {
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (org.apache.hc.core5.http.ParseException e) {
            throw new IOException("Failed to read response body", e);
        }
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
        applyAuth(request);
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
        applyAuth(request);
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
        logAudit(method, path, statusCode, 0, null);
    }

    private void logAudit(String method, String path, int statusCode, long durationMs, String errorMessage) {
        String key = Instant.now().toString() + "|" + method + "|" + path + "|" + statusCode;
        auditLog.put(key, method + " " + path + " -> " + statusCode);
        logger.info("AUDIT: {} {} -> {}", method, path, statusCode);

        // Persist to SQLite
        try {
            AuditLogger.getInstance().logApiCall(
                method,
                path,
                statusCode,
                durationMs,
                configManager.getActiveEnvironment(),
                null, // userId - could be extracted from auth
                null, // requestSize
                null, // responseSize
                errorMessage
            );
        } catch (Exception e) {
            logger.debug("Failed to persist audit log: {}", e.getMessage());
        }
    }

    public Map<String, String> getAuditLog() {
        return new ConcurrentHashMap<>(auditLog);
    }

    private void applyAuth(HttpRequest request) {
        if (authOverride != null && !authOverride.isEmpty()) {
            request.setHeader("Authorization", authOverride);
            return;
        }
        String token = configManager.getActiveAccessToken();
        String basicAuth = configManager.getActiveBasicAuth();

        if (basicAuth != null && !basicAuth.isEmpty()) {
            request.setHeader("Authorization", "Basic " + basicAuth);
        } else if (token != null && !token.isEmpty()) {
            request.setHeader("Authorization", "Bearer " + token);
        }
    }

    private String buildUrl(String path) {
        String baseUrl = baseUrlOverride != null ? baseUrlOverride : configManager.getActiveEnvironmentUrl();
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
