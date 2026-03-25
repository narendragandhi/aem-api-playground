package com.aemtools.aem.api;

import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PagesApi {

    private final AemApiClient client;
    private final ObjectMapper mapper;

    public PagesApi(AemApiClient client) {
        this.client = client;
        this.mapper = client.getObjectMapper();
    }

    public List<Page> list(String parentPath, int limit) throws IOException {
        String url = parentPath + ".1.json?type=cq:Page&p.limit=" + limit;
        
        JsonNode response = client.get(url);
        List<Page> pages = new ArrayList<>();
        
        if (response.has("data")) {
            ArrayNode data = (ArrayNode) response.get("data");
            for (JsonNode node : data) {
                pages.add(parsePage(node));
            }
        }
        
        return pages;
    }

    public Page get(String path) throws IOException {
        JsonNode response = client.get(path + ".json");
        return parsePage(response);
    }

    public Page getContent(String path) throws IOException {
        JsonNode response = client.get(path + "/jcr:content.json");
        
        Page page = new Page();
        page.setPath(path);
        page.setTitle(response.path("jcr:title").asText());
        page.setDescription(response.path("jcr:description").asText());
        
        if (response.has("root")) {
            page.setHasContent(true);
        }
        
        return page;
    }

    public Page create(String parentPath, String name, String template, String title) throws IOException {
        ObjectNode createRequest = mapper.createObjectNode();
        createRequest.put("jcr:primaryType", "cq:Page");
        createRequest.put("jcr:title", title != null ? title : name);
        createRequest.put("cq:template", template);
        
        String path = parentPath + "/" + name;
        JsonNode response = client.post("/api/pages" + path, createRequest);
        
        return parsePage(response);
    }

    public boolean delete(String path) throws IOException {
        return client.delete("/api/pages" + path);
    }

    public JsonNode update(String path, JsonNode content) throws IOException {
        return client.put("/api/pages" + path + "/jcr:content.json", content);
    }

    public JsonNode move(String sourcePath, String destPath) throws IOException {
        ObjectNode moveRequest = mapper.createObjectNode();
        moveRequest.put("dest", destPath);
        
        return client.post("/api/pages" + sourcePath + ".move.json", moveRequest);
    }

    public List<Page> search(String query, int limit) throws IOException {
        ObjectNode searchRequest = mapper.createObjectNode();
        searchRequest.put("text", query);
        searchRequest.put("type", "cq:Page");
        
        JsonNode response = client.post("/bin/cq/search.json", searchRequest);
        List<Page> pages = new ArrayList<>();
        
        if (response.has("hits")) {
            ArrayNode hits = (ArrayNode) response.get("hits");
            for (JsonNode hit : hits) {
                pages.add(parsePage(hit));
            }
        }
        
        return pages;
    }

    private Page parsePage(JsonNode node) {
        Page page = new Page();
        
        String path = node.path("path").asText();
        if (path.isEmpty()) {
            path = node.path("jcr:path").asText();
        }
        
        page.setPath(path);
        page.setName(node.path("name").asText());
        page.setTitle(node.path("title").asText(node.path("jcr:title").asText()));
        page.setDescription(node.path("description").asText(node.path("jcr:description").asText()));
        page.setTemplate(node.path("template").asText(node.path("cq:template").asText()));
        page.setCreated(node.path("created").asText(node.path("jcr:created").asText()));
        page.setModified(node.path("modified").asText(node.path("jcr:lastModified").asText()));
        
        return page;
    }

    public static class Page {
        private String path;
        private String name;
        private String title;
        private String description;
        private String template;
        private String created;
        private String modified;
        private boolean hasContent;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTemplate() { return template; }
        public void setTemplate(String template) { this.template = template; }
        public String getCreated() { return created; }
        public void setCreated(String created) { this.created = created; }
        public String getModified() { return modified; }
        public void setModified(String modified) { this.modified = modified; }
        public boolean isHasContent() { return hasContent; }
        public void setHasContent(boolean hasContent) { this.hasContent = hasContent; }

        @Override
        public String toString() {
            return String.format("%s - %s", name, title);
        }
    }
}
