package com.aemtools.aem.api;

import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReplicationApi {

    private final AemApiClient client;
    private final ObjectMapper mapper;

    public ReplicationApi(AemApiClient client) {
        this.client = client;
        this.mapper = client.getObjectMapper();
    }

    public boolean publish(String path, String agent) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("cmd", "activate");
        request.put("path", path);
        
        JsonNode response = client.post("/bin/replicate.json", request);
        
        return response.has("success") && response.get("success").asBoolean();
    }

    public boolean publish(List<String> paths) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("cmd", "activate");
        
        ArrayNode pathsNode = mapper.createArrayNode();
        for (String path : paths) {
            pathsNode.add(path);
        }
        request.set("paths", pathsNode);
        
        JsonNode response = client.post("/bin/replicate.json", request);
        
        return response.has("success") && response.get("success").asBoolean();
    }

    public boolean unpublish(String path, String agent) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("cmd", "deactivate");
        request.put("path", path);
        
        JsonNode response = client.post("/bin/replicate.json", request);
        
        return response.has("success") && response.get("success").asBoolean();
    }

    public boolean delete(String path) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("cmd", "delete");
        request.put("path", path);
        
        JsonNode response = client.post("/bin/replicate.json", request);
        
        return response.has("success") && response.get("success").asBoolean();
    }

    public ReplicationStatus getStatus(String path) throws IOException {
        JsonNode response = client.get("/bin/replicate.json?cmd=status&path=" + path);
        
        ReplicationStatus status = new ReplicationStatus();
        status.setPath(path);
        status.setPublished(response.path("published").asBoolean(false));
        status.setLastPublished(response.path("lastPublished").asText());
        status.setReplicationCount(response.path("count").asInt(0));
        
        return status;
    }

    public List<ReplicationAgent> listAgents() throws IOException {
        List<ReplicationAgent> agents = new ArrayList<>();
        
        JsonNode response = client.get("/bin/replicate.json?cmd=agents");
        
        if (response.isArray()) {
            for (JsonNode agentNode : response) {
                ReplicationAgent agent = new ReplicationAgent();
                agent.setId(agentNode.path("id").asText());
                agent.setName(agentNode.path("name").asText());
                agent.setEnabled(agentNode.path("enabled").asBoolean(true));
                agent.setTransportURI(agentNode.path("transportUri").asText());
                agents.add(agent);
            }
        }
        
        return agents;
    }

    public static class ReplicationStatus {
        private String path;
        private boolean published;
        private String lastPublished;
        private int replicationCount;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public boolean isPublished() { return published; }
        public void setPublished(boolean published) { this.published = published; }
        public String getLastPublished() { return lastPublished; }
        public void setLastPublished(String lastPublished) { this.lastPublished = lastPublished; }
        public int getReplicationCount() { return replicationCount; }
        public void setReplicationCount(int replicationCount) { this.replicationCount = replicationCount; }
    }

    public static class ReplicationAgent {
        private String id;
        private String name;
        private boolean enabled;
        private String transportURI;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getTransportURI() { return transportURI; }
        public void setTransportURI(String transportURI) { this.transportURI = transportURI; }
    }
}
