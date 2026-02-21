package com.example.aem.api;

import com.example.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ContentFragmentApi {

    private final AemApiClient client;
    private final ObjectMapper mapper;

    public ContentFragmentApi(AemApiClient client) {
        this.client = client;
        this.mapper = client.getObjectMapper();
    }

    public List<ContentFragment> list(String folderPath, int limit) throws IOException {
        String path = folderPath.endsWith("/") ? folderPath : folderPath + "/";
        String url = ".1.json?type=dam:ContentFragment&p.limit=" + limit;
        
        JsonNode response = client.get(path + url);
        List<ContentFragment> fragments = new ArrayList<>();
        
        if (response.has("data")) {
            ArrayNode data = (ArrayNode) response.get("data");
            for (JsonNode node : data) {
                fragments.add(parseFragment(node));
            }
        } else if (response.isArray()) {
            for (JsonNode node : response) {
                fragments.add(parseFragment(node));
            }
        }
        
        return fragments;
    }

    public ContentFragment get(String path) throws IOException {
        String apiPath = path + ".json";
        JsonNode response = client.get(apiPath);
        return parseFragment(response);
    }

    public ContentFragment create(String parentPath, String name, String modelPath, String title) throws IOException {
        ObjectNode createRequest = mapper.createObjectNode();
        createRequest.put("model", modelPath);
        createRequest.put("title", title != null ? title : name);
        
        String path = parentPath + "/" + name;
        JsonNode response = client.post("/api/content/fragments" + path, createRequest);
        return parseFragment(response);
    }

    public boolean delete(String path) throws IOException {
        return client.delete("/api/content/fragments" + path);
    }

    public JsonNode update(String path, JsonNode data) throws IOException {
        return client.put("/api/content/fragments" + path + ".json", data);
    }

    public List<ContentFragment> search(String query, int limit) throws IOException {
        ObjectNode searchRequest = mapper.createObjectNode();
        searchRequest.put("query", query);
        searchRequest.put("limit", limit);
        
        JsonNode response = client.post("/graphql/execute.json/my-project/all-fragments", searchRequest);
        List<ContentFragment> fragments = new ArrayList<>();
        
        if (response.has("data")) {
            JsonNode data = response.get("data");
            if (data.isArray()) {
                for (JsonNode node : data) {
                    fragments.add(parseFragment(node));
                }
            }
        }
        
        return fragments;
    }

    private ContentFragment parseFragment(JsonNode node) {
        ContentFragment cf = new ContentFragment();
        cf.setPath(node.path("path").asText());
        cf.setName(node.path("name").asText());
        cf.setTitle(node.path("title").asText());
        cf.setModel(node.path("model").asText());
        cf.setDescription(node.path("description").asText());
        cf.setCreated(node.path("created").asText());
        cf.setModified(node.path("modified").asText());
        return cf;
    }

    public static class ContentFragment {
        private String path;
        private String name;
        private String title;
        private String model;
        private String description;
        private String created;
        private String modified;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCreated() { return created; }
        public void setCreated(String created) { this.created = created; }
        public String getModified() { return modified; }
        public void setModified(String modified) { this.modified = modified; }

        @Override
        public String toString() {
            return String.format("%s - %s (%s)", name, title, path);
        }
    }
}
