package com.example.aem.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class OllamaClient {

    private final String baseUrl;
    private final String model;
    private final ObjectMapper mapper;
    private final OkHttpClient client;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public OllamaClient(String model) {
        this("http://localhost:11434", model);
    }

    public OllamaClient(String baseUrl, String model) {
        this.baseUrl = baseUrl;
        this.model = model != null ? model : "llama2";
        this.mapper = new ObjectMapper();
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    public boolean isAvailable() {
        try {
            Request request = new Request.Builder()
                .url(baseUrl + "/api/tags")
                .get()
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> listModels() throws IOException {
        Request request = new Request.Builder()
            .url(baseUrl + "/api/tags")
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to list models: " + response.code());
            }

            String body = response.body().string();
            JsonNode root = mapper.readTree(body);
            List<String> models = new ArrayList<>();

            if (root.has("models")) {
                ArrayNode modelsArray = (ArrayNode) root.get("models");
                for (JsonNode modelNode : modelsArray) {
                    models.add(modelNode.path("name").asText());
                }
            }

            return models;
        }
    }

    public String chat(String systemPrompt, List<Map<String, String>> messages) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("model", model);
        request.put("stream", false);

        ArrayNode messagesArray = mapper.createArrayNode();
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            ObjectNode systemMsg = mapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messagesArray.add(systemMsg);
        }

        for (Map<String, String> msg : messages) {
            ObjectNode msgNode = mapper.createObjectNode();
            msgNode.put("role", msg.get("role"));
            msgNode.put("content", msg.get("content"));
            messagesArray.add(msgNode);
        }

        request.set("messages", messagesArray);

        return executeRequest("/api/chat", request);
    }

    public String generate(String prompt) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("model", model);
        request.put("prompt", prompt);
        request.put("stream", false);

        return executeRequest("/api/generate", request);
    }

    public String generateWithOptions(String prompt, Map<String, Object> options) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("model", model);
        request.put("prompt", prompt);
        request.put("stream", false);

        if (options != null) {
            ObjectNode optsNode = mapper.createObjectNode();
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                optsNode.putPOJO(entry.getKey(), entry.getValue());
            }
            request.set("options", optsNode);
        }

        return executeRequest("/api/generate", request);
    }

    public String embeddings(String text) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("model", model);
        request.put("prompt", text);

        String response = executeRequest("/api/embeddings", request);
        
        JsonNode root = mapper.readTree(response);
        return root.path("embedding").toString();
    }

    private String executeRequest(String endpoint, ObjectNode requestBody) throws IOException {
        String json = mapper.writeValueAsString(requestBody);
        
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
            .url(baseUrl + endpoint)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Request failed: " + response.code() + " - " + response.message());
            }

            String responseBody = response.body().string();
            
            if (endpoint.equals("/api/chat")) {
                JsonNode root = mapper.readTree(responseBody);
                return root.path("message").path("content").asText();
            } else if (endpoint.equals("/api/generate")) {
                JsonNode root = mapper.readTree(responseBody);
                return root.path("response").asText();
            } else if (endpoint.equals("/api/embeddings")) {
                return responseBody;
            }
            
            return responseBody;
        }
    }

    public String pullModel() throws IOException {
        ObjectNode requestObj = mapper.createObjectNode();
        requestObj.put("name", model);
        requestObj.put("stream", false);

        String json = mapper.writeValueAsString(requestObj);
        RequestBody body = RequestBody.create(json, JSON);
        Request req = new Request.Builder()
            .url(baseUrl + "/api/pull")
            .post(body)
            .build();

        try (Response response = client.newCall(req).execute()) {
            if (response.isSuccessful()) {
                return "Model " + model + " pulled successfully";
            } else {
                return "Failed to pull model: " + response.code();
            }
        }
    }

    public String getModel() {
        return model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public static String getDefaultUrl() {
        return "http://localhost:11434";
    }

    public static List<String> getDefaultModels() {
        return Arrays.asList(
            "llama2",
            "llama2:7b",
            "llama2:13b",
            "llama3",
            "llama3:8b",
            "mistral",
            "codellama",
            "phi3",
            "qwen",
            "mistral"
        );
    }
}
