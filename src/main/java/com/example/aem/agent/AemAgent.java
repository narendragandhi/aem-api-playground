package com.example.aem.agent;

import com.example.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.*;

public class AemAgent {

    private final AemApiClient apiClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final LlmProvider provider;
    private final List<Map<String, String>> conversationHistory = new ArrayList<>();
    private AgentMemory memory = null;
    private boolean useCache = true;
    private boolean useMemory = true;
    private boolean memoryInitialized = false;

    public enum LlmProvider {
        OPENAI,
        OLLAMA
    }

    public AemAgent(String apiKey, String model, AemApiClient apiClient) {
        this.provider = detectProvider(apiKey);
        this.apiKey = apiKey;
        this.model = model;
        this.apiClient = apiClient;
        this.objectMapper = new ObjectMapper();
        
        if (useMemory) {
            try {
                memory = new AgentMemory();
                List<Map<String, String>> savedHistory = memory.loadHistory();
                conversationHistory.addAll(savedHistory);
            } catch (IOException e) {
                System.err.println("Warning: Memory initialization failed, using in-memory only: " + e.getMessage());
                memory = null;
            }
        }
    }

    private static final String SYSTEM_PROMPT = """
        You are an AEM (Adobe Experience Manager) API assistant. 
        You help users interact with AEM by converting their natural language requests into API calls.
        
        Available actions:
        - list_fragments: List content fragments
        - get_fragment: Get content fragment details
        - list_assets: List assets in a folder
        - upload_asset: Upload an asset
        - delete_asset: Delete an asset
        - list_pages: List pages
        - graphql_query: Execute GraphQL query
        - start_workflow: Start a workflow
        
        When user asks to do something, respond with a JSON action object:
        {"action": "action_name", "params": {"key": "value"}}
        
        If the user is just chatting, respond naturally without JSON.
        
        Current environment: %s
        """;

    public AemAgent(String apiKey, String model) {
        this(apiKey, model, (AemApiClient) null);
    }

    private LlmProvider detectProvider(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return LlmProvider.OLLAMA;
        }
        if (apiKey.startsWith("ollama:")) {
            return LlmProvider.OLLAMA;
        }
        if (System.getenv("OLLAMA_BASE_URL") != null) {
            return LlmProvider.OLLAMA;
        }
        return LlmProvider.OPENAI;
    }

    public String chat(String userMessage) {
        conversationHistory.add(Map.of("role", "user", "content", userMessage));

        String response;
        
        if (useCache && memory != null) {
            String cacheKey = memory.generateCacheKey(userMessage);
            String cached = memory.getFromCache(cacheKey);
            if (cached != null) {
                System.out.println("[Cache hit]");
                conversationHistory.add(Map.of("role", "assistant", "content", cached));
                return cached;
            }
        }

        try {
            if (provider == LlmProvider.OLLAMA) {
                response = chatWithOllama(userMessage);
            } else {
                response = chatWithOpenAI(userMessage);
            }
        } catch (Exception e) {
            response = "Error: " + e.getMessage();
        }

        if (useCache && memory != null && !response.startsWith("Error:")) {
            String cacheKey = memory.generateCacheKey(userMessage);
            memory.saveToCache(cacheKey, response);
        }

        conversationHistory.add(Map.of("role", "assistant", "content", response));

        if (useMemory && memoryInitialized && memory != null) {
            memory.saveHistory(conversationHistory);
        }

        return response;
    }

    private String chatWithOllama(String userMessage) throws IOException {
        String ollamaUrl = System.getenv("OLLAMA_BASE_URL");
        if (ollamaUrl == null) {
            ollamaUrl = "http://localhost:11434";
        }
        
        OllamaClient ollama = new OllamaClient(ollamaUrl, model);
        
        if (!ollama.isAvailable()) {
            return "Error: Ollama is not running. Start it with: ollama serve";
        }
        
        String systemPrompt = String.format(SYSTEM_PROMPT, envUrl());
        return ollama.chat(systemPrompt, conversationHistory);
    }

    private String chatWithOpenAI(String userMessage) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            return "Error: OpenAI API key required. Set OPENAI_API_KEY or use Ollama (--provider ollama)";
        }

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
    }

    private String envUrl() {
        try {
            return com.example.aem.config.ConfigManager.getInstance().getActiveEnvironmentUrl();
        } catch (Exception e) {
            return "not configured";
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
        com.example.aem.api.ContentFragmentApi cfApi = new com.example.aem.api.ContentFragmentApi(apiClient);
        var fragments = cfApi.list(path, 20);
        StringBuilder sb = new StringBuilder("Content Fragments:\n");
        for (var cf : fragments) {
            sb.append("  - ").append(cf.toString()).append("\n");
        }
        return sb.toString();
    }

    private String executeGetFragment(JsonNode params) throws IOException {
        String path = params.path("path").asText();
        com.example.aem.api.ContentFragmentApi cfApi = new com.example.aem.api.ContentFragmentApi(apiClient);
        var cf = cfApi.get(path);
        return "Content Fragment: " + cf.toString();
    }

    private String executeListAssets(JsonNode params) throws IOException {
        String path = params.path("path").asText("/content/dam");
        com.example.aem.api.AssetsApi assetsApi = new com.example.aem.api.AssetsApi(apiClient);
        var assets = assetsApi.list(path, 20);
        StringBuilder sb = new StringBuilder("Assets:\n");
        for (var asset : assets) {
            sb.append("  - ").append(asset.toString()).append("\n");
        }
        return sb.toString();
    }

    private String executeUploadAsset(JsonNode params) {
        return "Upload asset... (need file path implementation)";
    }

    private String executeDeleteAsset(JsonNode params) throws IOException {
        String path = params.path("path").asText();
        com.example.aem.api.AssetsApi assetsApi = new com.example.aem.api.AssetsApi(apiClient);
        boolean result = assetsApi.delete(path);
        return result ? "Asset deleted successfully" : "Failed to delete asset";
    }

    private String executeListPages(JsonNode params) throws IOException {
        String path = params.path("path").asText("/content");
        com.example.aem.api.PagesApi pagesApi = new com.example.aem.api.PagesApi(apiClient);
        var pages = pagesApi.list(path, 20);
        StringBuilder sb = new StringBuilder("Pages:\n");
        for (var page : pages) {
            sb.append("  - ").append(page.toString()).append("\n");
        }
        return sb.toString();
    }

    private String executeGraphQL(JsonNode params) throws IOException {
        String query = params.path("query").asText();
        com.example.aem.api.GraphQLApi graphqlApi = new com.example.aem.api.GraphQLApi(apiClient);
        JsonNode result = graphqlApi.executeQuery(query);
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
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
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isEmpty()) {
            key = System.getenv("AEM_API_KEY");
        }
        return key;
    }

    public LlmProvider getProvider() {
        return provider;
    }

    public boolean isOllamaAvailable() {
        if (provider == LlmProvider.OLLAMA) {
            try {
                OllamaClient ollama = new OllamaClient(model);
                return ollama.isAvailable();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
