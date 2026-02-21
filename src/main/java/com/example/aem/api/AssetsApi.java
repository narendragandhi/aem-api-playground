package com.example.aem.api;

import com.example.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AssetsApi {

    private final AemApiClient client;
    private final ObjectMapper mapper;

    public AssetsApi(AemApiClient client) {
        this.client = client;
        this.mapper = client.getObjectMapper();
    }

    public List<Asset> list(String folderPath, int limit) throws IOException {
        String url = folderPath + ".1.json?type=dam:Asset&p.limit=" + limit;
        
        JsonNode response = client.get(url);
        List<Asset> assets = new ArrayList<>();
        
        if (response.has("data")) {
            ArrayNode data = (ArrayNode) response.get("data");
            for (JsonNode node : data) {
                assets.add(parseAsset(node));
            }
        } else if (response.isArray()) {
            for (JsonNode node : response) {
                assets.add(parseAsset(node));
            }
        }
        
        return assets;
    }

    public Asset get(String path) throws IOException {
        JsonNode response = client.get(path + ".json");
        return parseAsset(response);
    }

    public Asset upload(String folderPath, String fileName, byte[] data, String mimeType) throws IOException {
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("name", fileName);
        metadata.put("mimeType", mimeType);
        metadata.put("title", fileName);
        
        String path = folderPath + "/" + fileName;
        
        try {
            JsonNode response = client.upload("/api/assets" + path, data, mimeType);
            return parseAsset(response);
        } catch (Exception e) {
            throw new IOException("Failed to upload asset: " + e.getMessage(), e);
        }
    }

    public Asset uploadFile(String folderPath, Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString();
        byte[] data = Files.readAllBytes(filePath);
        
        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        
        return upload(folderPath, fileName, data, mimeType);
    }

    public boolean delete(String path) throws IOException {
        return client.delete("/api/assets" + path);
    }

    public JsonNode move(String sourcePath, String destPath) throws IOException {
        ObjectNode moveRequest = mapper.createObjectNode();
        moveRequest.put("dest", destPath);
        moveRequest.put("replace", true);
        
        return client.post("/api/assets" + sourcePath + ".move.json", moveRequest);
    }

    public JsonNode copy(String sourcePath, String destPath) throws IOException {
        ObjectNode copyRequest = mapper.createObjectNode();
        copyRequest.put("dest", destPath);
        
        return client.post("/api/assets" + sourcePath + ".copy.json", copyRequest);
    }

    public byte[] download(String path) throws IOException {
        return client.download("/api/assets" + path + "?format=original");
    }

    public JsonNode updateMetadata(String path, JsonNode metadata) throws IOException {
        return client.post("/api/assets" + path + ".json", metadata);
    }

    public List<Asset> search(String query, int limit) throws IOException {
        ObjectNode searchRequest = mapper.createObjectNode();
        searchRequest.put("text", query);
        searchRequest.put("type", "dam:Asset");
        
        JsonNode response = client.post("/bin/cq/search.json", searchRequest);
        List<Asset> assets = new ArrayList<>();
        
        if (response.has("hits")) {
            ArrayNode hits = (ArrayNode) response.get("hits");
            for (JsonNode hit : hits) {
                assets.add(parseAsset(hit));
            }
        }
        
        return assets;
    }

    private Asset parseAsset(JsonNode node) {
        Asset asset = new Asset();
        asset.setPath(node.path("path").asText());
        asset.setName(node.path("name").asText());
        asset.setTitle(node.path("title").asText(node.path("jcr:title").asText()));
        asset.setMimeType(node.path("mimeType").asText());
        asset.setSize(node.path("size").asLong(0));
        asset.setCreated(node.path("created").asText(node.path("jcr:created").asText()));
        asset.setModified(node.path("modified").asText(node.path("jcr:lastModified").asText()));
        return asset;
    }

    public static class Asset {
        private String path;
        private String name;
        private String title;
        private String mimeType;
        private long size;
        private String created;
        private String modified;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public String getCreated() { return created; }
        public void setCreated(String created) { this.created = created; }
        public String getModified() { return modified; }
        public void setModified(String modified) { this.modified = modified; }

        @Override
        public String toString() {
            return String.format("%s - %s (%s, %s)", name, title, mimeType, formatSize(size));
        }

        private String formatSize(long size) {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
