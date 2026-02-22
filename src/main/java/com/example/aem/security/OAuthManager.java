package com.example.aem.security;

import com.example.aem.config.ConfigManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OAuthManager {

    private static final Logger logger = LoggerFactory.getLogger(OAuthManager.class);
    private static final String DEFAULT_SCOPE = "openid,profile,email,offline_access";
    private static final int TOKEN_REFRESH_BUFFER_SECONDS = 300;

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConfigManager configManager;
    private final ExecutorService executor;

    private String accessToken;
    private String refreshToken;
    private Instant tokenExpiresAt;
    private final Map<String, TokenListener> tokenListeners = new ConcurrentHashMap<>();

    public interface TokenListener {
        void onTokenRefreshed(String newToken);
    }

    public OAuthManager() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(5);
        cm.setDefaultMaxPerRoute(2);
        this.httpClient = HttpClients.custom()
            .setConnectionManager(cm)
            .build();
        this.objectMapper = new ObjectMapper();
        this.configManager = ConfigManager.getInstance();
        this.executor = Executors.newFixedThreadPool(2);
    }

    public OAuthManager(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
        this.configManager = ConfigManager.getInstance();
        this.executor = Executors.newFixedThreadPool(2);
    }

    public boolean isAuthenticated() {
        return accessToken != null && !isTokenExpired();
    }

    public boolean isTokenExpired() {
        if (accessToken == null || tokenExpiresAt == null) {
            return true;
        }
        return Instant.now().plusSeconds(TOKEN_REFRESH_BUFFER_SECONDS).isAfter(tokenExpiresAt);
    }

    public synchronized String getAccessToken() throws IOException {
        if (isAuthenticated()) {
            return accessToken;
        }

        if (refreshToken != null && !refreshToken.isEmpty()) {
            if (refreshAccessToken()) {
                return accessToken;
            }
        }

        if (configManager.getActiveEnvironmentUrl() != null) {
            performOAuthFlow();
        }

        return accessToken;
    }

    public void performOAuthFlow() throws IOException {
        String authUrl = configManager.getOAuthAuthorizationUrl();
        String tokenUrl = configManager.getOAuthTokenUrl();
        String clientId = configManager.getOAuthClientId();
        String clientSecret = configManager.getOAuthClientSecret();

        if (authUrl == null || tokenUrl == null || clientId == null) {
            logger.warn("OAuth configuration incomplete");
            return;
        }

        String authCode = promptForAuthCode(authUrl + "?client_id=" + clientId + "&redirect_uri=" + 
            configManager.getOAuthRedirectUri() + "&response_type=code&scope=" + DEFAULT_SCOPE);

        if (authCode == null) {
            throw new IOException("OAuth authorization failed");
        }

        exchangeCodeForToken(tokenUrl, clientId, clientSecret, authCode);
    }

    private String promptForAuthCode(String authUrl) {
        System.out.println("\n=== OAuth Authentication Required ===");
        System.out.println("Please open the following URL in your browser:");
        System.out.println(authUrl);
        System.out.println("\nEnter the authorization code: ");
        
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
            return reader.readLine().trim();
        } catch (IOException e) {
            logger.error("Failed to read authorization code", e);
            return null;
        }
    }

    private void exchangeCodeForToken(String tokenUrl, String clientId, String clientSecret, String authCode) throws IOException {
        HttpPost request = new HttpPost(tokenUrl);
        
        String body = "grant_type=authorization_code" +
            "&client_id=" + clientId +
            "&client_secret=" + clientSecret +
            "&code=" + authCode +
            "&redirect_uri=" + configManager.getOAuthRedirectUri();
        
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_FORM_URLENCODED));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody;
            try {
                responseBody = EntityUtils.toString(response.getEntity());
            } catch (ParseException e) {
                throw new IOException("Failed to parse response", e);
            }
            
            if (response.getCode() == 200) {
                JsonNode tokenJson = objectMapper.readTree(responseBody);
                this.accessToken = tokenJson.get("access_token").asText();
                this.refreshToken = tokenJson.has("refresh_token") ? 
                    tokenJson.get("refresh_token").asText() : null;
                
                int expiresIn = tokenJson.has("expires_in") ? tokenJson.get("expires_in").asInt() : 3600;
                this.tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
                
                logger.info("OAuth tokens obtained successfully");
                notifyTokenListeners();
            } else {
                throw new IOException("Token exchange failed: " + responseBody);
            }
        }
    }

    private boolean refreshAccessToken() throws IOException {
        String tokenUrl = configManager.getOAuthTokenUrl();
        String clientId = configManager.getOAuthClientId();
        String clientSecret = configManager.getOAuthClientSecret();

        if (tokenUrl == null || refreshToken == null) {
            return false;
        }

        HttpPost request = new HttpPost(tokenUrl);
        String body = "grant_type=refresh_token" +
            "&client_id=" + clientId +
            "&client_secret=" + clientSecret +
            "&refresh_token=" + refreshToken;
        
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_FORM_URLENCODED));
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseBody;
            try {
                responseBody = EntityUtils.toString(response.getEntity());
            } catch (ParseException e) {
                logger.warn("Failed to parse response", e);
                return false;
            }
            
            if (response.getCode() == 200) {
                JsonNode tokenJson = objectMapper.readTree(responseBody);
                this.accessToken = tokenJson.get("access_token").asText();
                
                if (tokenJson.has("refresh_token")) {
                    this.refreshToken = tokenJson.get("refresh_token").asText();
                }
                
                int expiresIn = tokenJson.has("expires_in") ? tokenJson.get("expires_in").asInt() : 3600;
                this.tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
                
                logger.info("OAuth token refreshed successfully");
                notifyTokenListeners();
                return true;
            } else {
                logger.warn("Token refresh failed: {}", responseBody);
                return false;
            }
        }
    }

    public void setTokens(String accessToken, String refreshToken, int expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
    }

    public void addTokenListener(String id, TokenListener listener) {
        tokenListeners.put(id, listener);
    }

    public void removeTokenListener(String id) {
        tokenListeners.remove(id);
    }

    private void notifyTokenListeners() {
        for (TokenListener listener : tokenListeners.values()) {
            try {
                listener.onTokenRefreshed(accessToken);
            } catch (Exception e) {
                logger.error("Error notifying token listener", e);
            }
        }
    }

    public void clearTokens() {
        this.accessToken = null;
        this.refreshToken = null;
        this.tokenExpiresAt = null;
        logger.info("OAuth tokens cleared");
    }

    public Map<String, Object> getTokenInfo() {
        Map<String, Object> info = new ConcurrentHashMap<>();
        info.put("authenticated", isAuthenticated());
        info.put("expiresAt", tokenExpiresAt != null ? tokenExpiresAt.toString() : null);
        info.put("hasRefreshToken", refreshToken != null && !refreshToken.isEmpty());
        
        if (tokenExpiresAt != null) {
            long secondsUntilExpiry = tokenExpiresAt.getEpochSecond() - Instant.now().getEpochSecond();
            info.put("secondsUntilExpiry", Math.max(0, secondsUntilExpiry));
        }
        
        return info;
    }

    public void shutdown() {
        executor.shutdown();
        try {
            httpClient.close();
        } catch (IOException e) {
            logger.error("Error closing HTTP client", e);
        }
    }
}
