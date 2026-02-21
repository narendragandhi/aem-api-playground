package com.example.aem.agent;

import com.example.aem.client.AemApiClient;
import com.example.aem.config.ConfigManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AemAgent {

    private final AemApiClient apiClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final List<Map<String, String>> conversationHistory = new ArrayList<>();
    private final AgentMemory memory;
    private boolean useCache = true;
    private boolean useMemory = true;
    private boolean memoryInitialized = false;

    private static final String SYSTEM_PROMPT = """
        You are an AEM (Adobe Experience Manager) API assistant. 
        You help users interact with AEM by converting their natural language requests into API calls.
        
        Available actions:
        - List content fragments: GET /api/content/fragments
        - Get content fragment: GET /api/content/fragments/{path}
        - Create content fragment: POST /api/content/fragments
        - List assets: GET /api/assets
        - Upload asset: POST /api/assets
        - Delete asset: DELETE /api/assets/{path}
        - List pages: GET /api/pages
        - List forms: GET /api/forms
        - Execute GraphQL: POST /graphql/execute
        - Start workflow: POST /bin/workflow
        
        When user asks to do something, respond with a JSON action object:
        {"action": "action_name", "params": {"key": "value"}}
        
        If the user is just chatting, respond naturally without JSON.
        
        Current environment: %s
        """;

    public AemAgent(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model != null ? model : "gpt-4";
        this.apiClient = new AemApiClient();
        this.objectMapper = new ObjectMapper();
        
        AgentMemory mem = null;
        try {
            mem = new AgentMemory();
            memoryInitialized = true;
            if (useMemory) {
                List<Map<String, String>> savedHistory = mem.loadHistory();
                conversationHistory.addAll(savedHistory);
            }
        } catch (IOException e) {
            System.err.println("Warning: Memory initialization failed, using in-memory only: " + e.getMessage());
            mem = null;
        }
        this.memory = mem;
    }

    public String chat(String userMessage) {
        conversationHistory.add(Map.of("role", "user", "content", userMessage));

        String prompt = buildPrompt(userMessage);
        
        String response;
        if (useCache) {
            String cacheKey = memory != null ? memory.generateCacheKey(prompt) : null;
            if (cacheKey != null && memory.getFromCache(cacheKey) != null) {
                System.out.println("[Cache hit]");
                response = memory.getFromCache(cacheKey);
            } else {
                response = callOpenAI(prompt);
                if (cacheKey != null && memory != null) {
                    memory.saveToCache(cacheKey, response);
                }
            }
        } else {
            response = callOpenAI(prompt);
        }

        conversationHistory.add(Map.of("role", "assistant", "content", response));

        if (useMemory && memoryInitialized && memory != null) {
            memory.saveHistory(conversationHistory);
        }

        return response;
    }

    private String buildPrompt(String userMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(SYSTEM_PROMPT, envUrl()));
        sb.append("\n\n");
        
        for (Map<String, String> msg : conversationHistory) {
            sb.append(msg.get("role")).append(": ").append(msg.get("content")).append("\n");
        }
        
        sb.append("assistant:");
        return sb.toString();
    }

    private String envUrl() {
        try {
            return ConfigManager.getInstance().getActiveEnvironmentUrl();
        } catch (Exception e) {
            return "not configured";
        }
    }

    private String callOpenAI(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "Error: OpenAI API key not configured. Use 'aem-api agent --api-key <key>' or set OPENAI_API_KEY environment variable.";
        }

        try {
            OkHttpClient client = new OkHttpClient();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", conversationHistory);
            requestBody.put("temperature", 0.7);

            RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(requestBody),
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "Error: OpenAI API returned " + response.code();
                }

                JsonNode root = objectMapper.readTree(response.body().string());
                return root.path("choices").get(0).path("message").path("content").asText();
            }
        } catch (Exception e) {
            return "Error calling OpenAI: " + e.getMessage();
        }
    }

    public String executeAction(String actionJson) {
        try {
            JsonNode action = objectMapper.readTree(actionJson);
            String actionName = action.get("action").asText();
            JsonNode params = action.get("params");

            return switch (actionName) {
                case "list_fragments" -> executeListFragments(params);
                case "get_fragment" -> executeGetFragment(params);
                case "list_assets" -> executeListAssets(params);
                case "upload_asset" -> executeUploadAsset(params);
                case "delete_asset" -> executeDeleteAsset(params);
                case "list_pages" -> executeListPages(params);
                case "graphql_query" -> executeGraphQL(params);
                case "start_workflow" -> executeStartWorkflow(params);
                default -> "Unknown action: " + actionName;
            };
        } catch (Exception e) {
            return "Error executing action: " + e.getMessage();
        }
    }

    private String executeListFragments(JsonNode params) throws IOException {
        String path = params.path("path").asText("/content/dam");
        JsonNode result = apiClient.get("/api/content/fragments" + path + ".1.json");
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    private String executeGetFragment(JsonNode params) throws IOException {
        String path = params.path("path").asText();
        JsonNode result = apiClient.get("/api/content/fragments" + path + ".json");
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    private String executeListAssets(JsonNode params) {
        return "Listing assets... (not implemented)";
    }

    private String executeUploadAsset(JsonNode params) {
        return "Uploading asset... (not implemented)";
    }

    private String executeDeleteAsset(JsonNode params) throws IOException {
        String path = params.path("path").asText();
        boolean result = apiClient.delete("/api/assets" + path);
        return result ? "Asset deleted successfully" : "Failed to delete asset";
    }

    private String executeListPages(JsonNode params) {
        return "Listing pages... (not implemented)";
    }

    private String executeGraphQL(JsonNode params) {
        return "Executing GraphQL... (not implemented)";
    }

    private String executeStartWorkflow(JsonNode params) {
        return "Starting workflow... (not implemented)";
    }

    public void clearHistory() {
        conversationHistory.clear();
        if (memory != null) {
            memory.clearAll();
        }
    }

    public Map<String, Object> getMemoryStats() {
        if (memory != null) {
            return memory.getStats();
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("history_count", conversationHistory.size());
        stats.put("cache_enabled", useCache);
        stats.put("persistence", "disabled");
        return stats;
    }

    public void saveSession(String name) {
        if (memory != null) {
            memory.saveSession(name, conversationHistory);
        }
    }

    public void loadSession(String name) {
        if (memory != null) {
            List<Map<String, String>> session = memory.loadSession(name);
            if (!session.isEmpty()) {
                conversationHistory.clear();
                conversationHistory.addAll(session);
            }
        }
    }

    public List<String> listSessions() {
        if (memory != null) {
            return memory.listSessions();
        }
        return new ArrayList<>();
    }

    public void deleteSession(String name) {
        if (memory != null) {
            memory.deleteSession(name);
        }
    }

    public void setCacheEnabled(boolean enabled) {
        this.useCache = enabled;
    }

    public void setMemoryEnabled(boolean enabled) {
        this.useMemory = enabled;
    }

    public void clearCache() {
        if (memory != null) {
            memory.clearCache();
        }
    }

    public static String getApiKey() {
        return System.getenv("OPENAI_API_KEY");
    }
}
