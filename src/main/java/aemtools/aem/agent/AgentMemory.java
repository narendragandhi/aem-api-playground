package com.aemtools.aem.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AgentMemory {

    private final Path memoryDir;
    private final Path historyFile;
    private final Path cacheFile;
    private final Path sessionsDir;
    private final ObjectMapper objectMapper;
    private final Map<String, String> responseCache;
    private static final long CACHE_TTL_MS = 3600000;
    private static final int MAX_HISTORY = 100;
    private static final int MAX_CACHE = 1000;

    public AgentMemory() throws IOException {
        String homeDir = System.getProperty("user.home");
        this.memoryDir = Paths.get(homeDir, ".aem-api", "agent");
        this.historyFile = memoryDir.resolve("history.json");
        this.cacheFile = memoryDir.resolve("cache.json");
        this.sessionsDir = memoryDir.resolve("sessions");
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.responseCache = new ConcurrentHashMap<>();

        Files.createDirectories(memoryDir);
        Files.createDirectories(sessionsDir);

        loadCache();
    }

    public void saveHistory(List<Map<String, String>> history) {
        try {
            List<Map<String, String>> trimmed = history.size() > MAX_HISTORY
                ? history.subList(history.size() - MAX_HISTORY, history.size())
                : history;
            objectMapper.writeValue(historyFile.toFile(), trimmed);
        } catch (IOException e) {
            System.err.println("Warning: Could not save history: " + e.getMessage());
        }
    }

    public List<Map<String, String>> loadHistory() {
        if (!historyFile.toFile().exists()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(historyFile.toFile(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public void saveToCache(String key, String response) {
        String cacheEntry = System.currentTimeMillis() + "|" + response;
        responseCache.put(key, cacheEntry);
        
        if (responseCache.size() > MAX_CACHE) {
            cleanCache();
        }
        
        saveCache();
    }

    public String getFromCache(String key) {
        String entry = responseCache.get(key);
        if (entry == null) return null;

        String[] parts = entry.split("\\|", 2);
        long timestamp = Long.parseLong(parts[0]);
        
        if (System.currentTimeMillis() - timestamp > CACHE_TTL_MS) {
            responseCache.remove(key);
            return null;
        }
        
        return parts[1];
    }

    public String generateCacheKey(String prompt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(prompt.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return prompt.hashCode() + "";
        }
    }

    private void loadCache() {
        if (!cacheFile.toFile().exists()) return;
        try {
            JsonNode root = objectMapper.readTree(cacheFile.toFile());
            if (root.isObject()) {
                root.fields().forEachRemaining(entry -> {
                    responseCache.put(entry.getKey(), entry.getValue().asText());
                });
            }
            cleanCache();
        } catch (IOException e) {
            System.err.println("Warning: Could not load cache: " + e.getMessage());
        }
    }

    private void saveCache() {
        try {
            objectMapper.writeValue(cacheFile.toFile(), responseCache);
        } catch (IOException e) {
            System.err.println("Warning: Could not save cache: " + e.getMessage());
        }
    }

    private void cleanCache() {
        long now = System.currentTimeMillis();
        responseCache.entrySet().removeIf(entry -> {
            String[] parts = entry.getValue().split("\\|", 2);
            if (parts.length < 2) return true;
            try {
                return (now - Long.parseLong(parts[0])) > CACHE_TTL_MS;
            } catch (NumberFormatException e) {
                return true;
            }
        });
    }

    public void saveSession(String sessionName, List<Map<String, String>> history) {
        try {
            Path sessionFile = sessionsDir.resolve(sessionName + ".json");
            objectMapper.writeValue(sessionFile.toFile(), history);
        } catch (IOException e) {
            System.err.println("Warning: Could not save session: " + e.getMessage());
        }
    }

    public List<Map<String, String>> loadSession(String sessionName) {
        try {
            Path sessionFile = sessionsDir.resolve(sessionName + ".json");
            if (sessionFile.toFile().exists()) {
                return objectMapper.readValue(sessionFile.toFile(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load session: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public List<String> listSessions() {
        try {
            return Files.list(sessionsDir)
                .filter(p -> p.toString().endsWith(".json"))
                .map(p -> p.getFileName().toString().replace(".json", ""))
                .toList();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public void deleteSession(String sessionName) {
        try {
            Files.deleteIfExists(sessionsDir.resolve(sessionName + ".json"));
        } catch (IOException e) {
            System.err.println("Warning: Could not delete session: " + e.getMessage());
        }
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("history_count", loadHistory().size());
        stats.put("cache_size", responseCache.size());
        stats.put("cache_ttl_seconds", CACHE_TTL_MS / 1000);
        stats.put("sessions", listSessions());
        stats.put("memory_dir", memoryDir.toString());
        return stats;
    }

    public void clearCache() {
        responseCache.clear();
        try {
            Files.deleteIfExists(cacheFile);
        } catch (IOException e) {
            System.err.println("Warning: Could not clear cache: " + e.getMessage());
        }
    }

    public void clearAll() {
        clearCache();
        try {
            Files.deleteIfExists(historyFile);
        } catch (IOException e) {
            System.err.println("Warning: Could not clear history: " + e.getMessage());
        }
    }
}
