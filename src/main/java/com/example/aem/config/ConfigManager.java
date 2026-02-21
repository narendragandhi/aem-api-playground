package com.example.aem.config;

import com.example.aem.security.CredentialEncryption;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private static ConfigManager instance;
    private final Map<String, Map<String, String>> environments = new HashMap<>();
    private String activeEnvironment = "dev";
    private boolean debugEnabled = false;
    private final File configFile;

    private ConfigManager() {
        String configDir = System.getProperty("user.home") + "/.aem-api";
        this.configFile = new File(configDir + "/config.yaml");
        load();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public void load() {
        if (configFile.exists()) {
            try {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                Map<String, Object> data = mapper.readValue(configFile, Map.class);
                
                if (data != null) {
                    Object envsObj = data.get("environments");
                    if (envsObj instanceof Map) {
                        Map<String, Object> envsMap = (Map<String, Object>) envsObj;
                        for (Map.Entry<String, Object> entry : envsMap.entrySet()) {
                            if (entry.getValue() instanceof Map) {
                                Map<String, String> envConfig = new HashMap<>();
                                Map<String, Object> rawConfig = (Map<String, Object>) entry.getValue();
                                for (Map.Entry<String, Object> configEntry : rawConfig.entrySet()) {
                                    if (configEntry.getValue() != null) {
                                        envConfig.put(configEntry.getKey(), configEntry.getValue().toString());
                                    }
                                }
                                environments.put(entry.getKey(), envConfig);
                            }
                        }
                    }
                    
                    Object activeEnv = data.get("activeEnvironment");
                    if (activeEnv != null) {
                        activeEnvironment = activeEnv.toString();
                    }
                }
            } catch (IOException e) {
                System.err.println("Warning: Could not load config: " + e.getMessage());
            }
        }
        if (environments.isEmpty()) {
            environments.put("dev", new HashMap<>());
            environments.put("staging", new HashMap<>());
            environments.put("prod", new HashMap<>());
        }
    }

    public void save() {
        try {
            configFile.getParentFile().mkdirs();
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            
            Map<String, Object> data = new HashMap<>();
            data.put("environments", environments);
            data.put("activeEnvironment", activeEnvironment);
            
            mapper.writeValue(configFile, data);
        } catch (IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }

    public void setEnvironmentUrl(String env, String url) {
        getOrCreateEnvironment(env).put("url", url);
    }

    public void setAccessToken(String env, String token) {
        getOrCreateEnvironment(env).put("accessToken", CredentialEncryption.encrypt(token));
    }

    public String getActiveAccessToken() {
        Map<String, String> env = environments.get(activeEnvironment);
        if (env == null) return null;
        String token = env.get("accessToken");
        if (token == null) return null;
        if (CredentialEncryption.isEncrypted(token)) {
            return CredentialEncryption.decrypt(token);
        }
        return token;
    }

    public void setClientSecret(String env, String clientSecret) {
        getOrCreateEnvironment(env).put("clientSecret", CredentialEncryption.encrypt(clientSecret));
    }

    public String getClientSecret(String env) {
        Map<String, String> envConfig = environments.get(env);
        if (envConfig == null) return null;
        String secret = envConfig.get("clientSecret");
        if (secret == null) return null;
        if (CredentialEncryption.isEncrypted(secret)) {
            return CredentialEncryption.decrypt(secret);
        }
        return secret;
    }

    public void setActiveEnvironment(String env) {
        if (environments.containsKey(env)) {
            this.activeEnvironment = env;
        } else {
            System.out.println("Warning: Environment '" + env + "' not configured. Creating new entry.");
            this.activeEnvironment = env;
            getOrCreateEnvironment(env);
        }
    }

    public String getActiveEnvironment() {
        return activeEnvironment;
    }

    public String getActiveEnvironmentUrl() {
        Map<String, String> env = environments.get(activeEnvironment);
        return env != null ? env.get("url") : null;
    }

    public void setImsEndpoint(String env, String endpoint) {
        getOrCreateEnvironment(env).put("imsEndpoint", endpoint);
    }

    public void setClientId(String env, String clientId) {
        getOrCreateEnvironment(env).put("clientId", clientId);
    }

    public void setBasicAuth(String env, String encodedBasicAuth) {
        getOrCreateEnvironment(env).put("basicAuth", CredentialEncryption.encrypt(encodedBasicAuth));
    }

    public String getActiveBasicAuth() {
        Map<String, String> env = environments.get(activeEnvironment);
        if (env == null) return null;
        String auth = env.get("basicAuth");
        if (auth == null) return null;
        if (CredentialEncryption.isEncrypted(auth)) {
            return CredentialEncryption.decrypt(auth);
        }
        return auth;
    }

    private Map<String, String> getOrCreateEnvironment(String env) {
        return environments.computeIfAbsent(env, k -> new HashMap<>());
    }

    public void listEnvironments() {
        System.out.println("\nConfigured environments:");
        for (Map.Entry<String, Map<String, String>> entry : environments.entrySet()) {
            String marker = entry.getKey().equals(activeEnvironment) ? " *" : "  ";
            String url = entry.getValue().get("url");
            System.out.println(marker + " " + entry.getKey() + ": " + (url != null ? url : "(not set)"));
        }
        System.out.println(" (* = active)");
    }

    public void showConfig() {
        System.out.println("\nCurrent Configuration:");
        System.out.println("  Active environment: " + activeEnvironment);
        System.out.println("  Debug mode: " + (debugEnabled ? "ON" : "OFF"));
        
        Map<String, String> env = environments.get(activeEnvironment);
        if (env != null) {
            System.out.println("\n  Environment '" + activeEnvironment + "' details:");
            System.out.println("    URL: " + (env.get("url") != null ? env.get("url") : "(not set)"));
            String token = env.get("accessToken");
            System.out.println("    Access Token: " + (token != null ? "***encrypted***" : "(not set)"));
            System.out.println("    IMS Endpoint: " + (env.get("imsEndpoint") != null ? env.get("imsEndpoint") : "(default)"));
            System.out.println("    Client ID: " + (env.get("clientId") != null ? env.get("clientId") : "(not set)"));
            System.out.println("    Client Secret: " + (env.get("clientSecret") != null ? "***encrypted***" : "(not set)"));
            System.out.println("    Basic Auth: " + (env.get("basicAuth") != null ? "***encrypted***" : "(not set)"));
        }
        System.out.println();
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void toggleDebug() {
        this.debugEnabled = !this.debugEnabled;
    }

    public java.util.Set<String> getEnvironmentNames() {
        return environments.keySet();
    }
}
