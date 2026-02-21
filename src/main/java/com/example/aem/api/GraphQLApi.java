package com.example.aem.api;

import com.example.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraphQLApi {

    private final AemApiClient client;
    private final ObjectMapper mapper;

    public GraphQLApi(AemApiClient client) {
        this.client = client;
        this.mapper = client.getObjectMapper();
    }

    public JsonNode executeQuery(String query) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("query", query);
        
        return client.post("/graphql/execute.json", request);
    }

    public JsonNode executeQuery(String endpoint, String query, Map<String, Object> variables) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("query", query);
        
        if (variables != null && !variables.isEmpty()) {
            ObjectNode varsNode = mapper.createObjectNode();
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                varsNode.putPOJO(entry.getKey(), entry.getValue());
            }
            request.set("variables", varsNode);
        }
        
        String path = "/graphql/execute.json/" + endpoint;
        return client.post(path, request);
    }

    public JsonNode executePersistedQuery(String persistedQueryName) throws IOException {
        String path = "/graphql/execute.json/" + persistedQueryName;
        return client.get(path);
    }

    public JsonNode executePersistedQuery(String endpoint, String persistedQueryName) throws IOException {
        String path = "/graphql/execute.json/" + endpoint + "/" + persistedQueryName;
        return client.get(path);
    }

    public JsonNode executePersistedQuery(String endpoint, String persistedQueryName, Map<String, Object> variables) throws IOException {
        String varsParam = "";
        if (variables != null && !variables.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                if (sb.length() > 0) sb.append("&");
                sb.append(entry.getKey()).append("=").append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
            }
            varsParam = "?" + sb;
        }
        
        String path = "/graphql/execute.json/" + endpoint + "/" + persistedQueryName + varsParam;
        return client.get(path);
    }

    public List<PersistedQuery> listPersistedQueries() throws IOException {
        List<PersistedQuery> queries = new ArrayList<>();
        
        try {
            JsonNode response = client.get("/graphql/persisted-query.json");
            
            if (response.has("paths")) {
                ArrayNode paths = (ArrayNode) response.get("paths");
                for (JsonNode pathNode : paths) {
                    String queryPath = pathNode.asText();
                    PersistedQuery pq = new PersistedQuery();
                    pq.setPath(queryPath);
                    
                    String[] parts = queryPath.split("/");
                    if (parts.length >= 3) {
                        pq.setEndpoint(parts[2]);
                        pq.setName(parts[3]);
                    }
                    
                    queries.add(pq);
                }
            }
        } catch (Exception e) {
            System.err.println("Note: Could not list persisted queries: " + e.getMessage());
        }
        
        return queries;
    }

    public JsonNode getPersistedQuery(String path) throws IOException {
        return client.get(path + ".json");
    }

    public JsonNode createPersistedQuery(String endpoint, String name, String query) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("query", query);
        
        String path = "/graphql/persisted-query/" + endpoint + "/" + name;
        return client.post(path, request);
    }

    public JsonNode createPersistedQuery(String endpoint, String name, String query, boolean cache) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("query", query);
        request.put("cache", cache);
        
        String path = "/graphql/persisted-query/" + endpoint + "/" + name;
        return client.post(path, request);
    }

    public boolean deletePersistedQuery(String path) throws IOException {
        return client.delete(path);
    }

    public JsonNode introspect(String endpoint) throws IOException {
        String query = "{ __schema { types { name kind fields { name } } } }";
        return executeQuery(endpoint, query, null);
    }

    public static class PersistedQuery {
        private String path;
        private String endpoint;
        private String name;
        private String query;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        @Override
        public String toString() {
            return String.format("%s/%s", endpoint, name);
        }
    }
}
