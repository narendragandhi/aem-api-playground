package com.aemtools.aem.api;

import com.aemtools.aem.client.AemApiClient;
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

    private static final String API_BASE = "/api/assets";
    
    private final AemApiClient client;
    private final ObjectMapper mapper;

    public AssetsApi(AemApiClient client) {
        this.client = client;
        this.mapper = client.getObjectMapper();
    }

    public List<Asset> list(String folderPath, int limit) throws IOException {
        String apiPath = normalizePath(folderPath);
        String url = API_BASE + apiPath + ".1.json";
        if (limit > 0) {
            url += (url.contains("?") ? "&" : "?") + "limit=" + limit;
        }
        
        JsonNode response = client.get(url);
        List<Asset> assets = new ArrayList<>();
        
        if (response.has("entities")) {
            ArrayNode entities = (ArrayNode) response.get("entities");
            for (JsonNode entity : entities) {
                String className = "";
                JsonNode classNode = entity.get("class");
                if (classNode != null) {
                    if (classNode.isArray()) {
                        className = classNode.elements().hasNext() ? classNode.elements().next().asText() : "";
                    } else {
                        className = classNode.asText();
                    }
                }
                
                if ("asset".equals(className) || "assets/asset".equals(className)) {
                    assets.add(parseAsset(entity));
                } else if (className.contains("folder")) {
                    Asset folderAsset = new Asset();
                    folderAsset.setName(entity.path("properties").path("name").asText());
                    folderAsset.setTitle(entity.path("properties").path("dc:title").asText(folderAsset.getName()));
                    folderAsset.setMimeType("folder");
                    assets.add(folderAsset);
                }
            }
        } else if (response.isArray()) {
            for (JsonNode node : response) {
                assets.add(parseAsset(node));
            }
        }
        
        return assets;
    }

    public List<Folder> listFolders(String folderPath) throws IOException {
        String apiPath = normalizePath(folderPath);
        String url = API_BASE + apiPath + ".1.json";
        
        JsonNode response = client.get(url);
        List<Folder> folders = new ArrayList<>();
        
        if (response.has("entities")) {
            ArrayNode entities = (ArrayNode) response.get("entities");
            for (JsonNode entity : entities) {
                String className = "";
                JsonNode classNode = entity.get("class");
                if (classNode != null) {
                    if (classNode.isArray()) {
                        className = classNode.elements().hasNext() ? classNode.elements().next().asText() : "";
                    } else {
                        className = classNode.asText();
                    }
                }
                
                if (className.contains("folder")) {
                    folders.add(parseFolder(entity));
                }
            }
        }
        
        return folders;
    }

    public Asset get(String path) throws IOException {
        String apiPath = normalizePath(path);
        JsonNode response = client.get(API_BASE + apiPath + ".json");
        return parseAsset(response);
    }

    public Folder createFolder(String parentPath, String folderName, String title) throws IOException {
        ObjectNode folderRequest = mapper.createObjectNode();
        folderRequest.put("jcr:primaryType", "sling:Folder");
        folderRequest.put("jcr:title", title != null ? title : folderName);
        
        String normalizedPath = normalizePath(parentPath);
        if (!normalizedPath.isEmpty() && !normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        
        String url = API_BASE + normalizedPath + "/" + folderName;
        
        try {
            JsonNode response = client.post(url, folderRequest);
            return parseFolder(response);
        } catch (Exception e) {
            throw new IOException("Failed to create folder: " + e.getMessage(), e);
        }
    }

    public boolean deleteFolder(String path) throws IOException {
        String apiPath = normalizePath(path);
        return client.delete(API_BASE + apiPath);
    }

    public int moveBatch(List<String> sourcePaths, String targetFolder) throws IOException {
        int moved = 0;
        for (String sourcePath : sourcePaths) {
            try {
                String sourceApiPath = normalizePath(sourcePath);
                String targetApiPath = normalizePath(targetFolder) + "/" + sourcePath.substring(sourcePath.lastIndexOf("/") + 1);
                
                ObjectNode moveRequest = mapper.createObjectNode();
                moveRequest.put("operation", "move");
                moveRequest.put("destination", targetApiPath);
                
                client.post(API_BASE + sourceApiPath + ".move.json", moveRequest);
                moved++;
            } catch (Exception e) {
                System.err.println("Failed to move " + sourcePath + ": " + e.getMessage());
            }
        }
        return moved;
    }

    public Asset upload(String folderPath, String fileName, byte[] data, String mimeType) throws IOException {
        String apiPath = normalizePath(folderPath);
        ObjectNode request = mapper.createObjectNode();
        request.put("class", "asset");
        
        ObjectNode properties = mapper.createObjectNode();
        properties.put("name", fileName);
        properties.put("dc:title", fileName);
        request.set("properties", properties);
        
        String url = API_BASE + apiPath + "/" + fileName;
        
        try {
            JsonNode response = client.upload(url, data, "application/octet-stream");
            return parseAsset(response);
        } catch (Exception e) {
            throw new IOException("Failed to upload asset: " + e.getMessage(), e);
        }
    }

    public Asset uploadFile(String folderPath, java.nio.file.Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString();
        byte[] data = java.nio.file.Files.readAllBytes(filePath);
        
        String mimeType = java.nio.file.Files.probeContentType(filePath);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        
        return upload(folderPath, fileName, data, mimeType);
    }

    public boolean deleteAsset(String path) throws IOException {
        String apiPath = normalizePath(path);
        return client.delete(API_BASE + apiPath);
    }

    public boolean delete(String path) throws IOException {
        return deleteAsset(path);
    }

    public Asset updateMetadata(String path, JsonNode metadata) throws IOException {
        String apiPath = normalizePath(path);
        ObjectNode request = mapper.createObjectNode();
        request.put("class", "asset");
        request.set("properties", metadata);
        
        JsonNode response = client.put(API_BASE + apiPath + ".json", request);
        return parseAsset(response);
    }

    public JsonNode move(String sourcePath, String destPath) throws IOException {
        String sourceApiPath = normalizePath(sourcePath);
        String destApiPath = normalizePath(destPath);
        
        return client.move(API_BASE + sourceApiPath, API_BASE + destApiPath);
    }

    public JsonNode copy(String sourcePath, String destPath) throws IOException {
        String sourceApiPath = normalizePath(sourcePath);
        String destApiPath = normalizePath(destPath);
        
        return client.copy(API_BASE + sourceApiPath, API_BASE + destApiPath);
    }

    public List<Asset> search(String query, int limit) throws IOException {
        ObjectNode searchRequest = mapper.createObjectNode();
        searchRequest.put("text", query);
        
        JsonNode response = client.post("/graphql/execute.json/aem/search-assets", searchRequest);
        List<Asset> assets = new ArrayList<>();
        
        if (response.has("data") && response.get("data").has("assetSearch")) {
            JsonNode results = response.get("data").get("assetSearch");
            if (results.isArray()) {
                for (JsonNode node : results) {
                    assets.add(parseAsset(node));
                }
            }
        }
        
        return assets;
    }

    public JsonNode createRendition(String assetPath, String renditionName, byte[] data, String mimeType) throws IOException {
        String apiPath = normalizePath(assetPath);
        String url = API_BASE + apiPath + "/renditions/" + renditionName;
        
        return client.upload(url, data, mimeType);
    }

    public JsonNode addComment(String assetPath, String message) throws IOException {
        String apiPath = normalizePath(assetPath);
        ObjectNode commentRequest = mapper.createObjectNode();
        commentRequest.put("message", message);
        
        return client.post(API_BASE + apiPath + "/comments", commentRequest);
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.startsWith("/api/assets")) {
            return path.replace("/api/assets", "");
        }
        if (path.startsWith("/content/dam")) {
            return path.replace("/content/dam", "");
        }
        if (path.startsWith("content/dam")) {
            return "/" + path.replace("content/dam", "");
        }
        return path;
    }

    private Asset parseAsset(JsonNode node) {
        Asset asset = new Asset();
        
        if (node.has("links")) {
            for (JsonNode link : node.get("links")) {
                if ("self".equals(link.path("rel").asText())) {
                    String href = link.path("href").asText();
                    asset.setPath(href);
                }
            }
        }
        
        if (node.has("properties")) {
            JsonNode props = node.get("properties");
            asset.setName(props.path("name").asText());
            asset.setTitle(props.path("dc:title").asText(props.path("name").asText()));
            asset.setMimeType(props.path("dc:format").asText());
            asset.setDescription(props.path("dc:description").asText());
            asset.setCreated(props.path("jcr:created").asText());
            asset.setModified(props.path("jcr:lastModified").asText());
        }
        
        if (asset.getName().isEmpty()) {
            asset.setName(node.path("name").asText());
        }
        
        return asset;
    }

    private Folder parseFolder(JsonNode node) {
        Folder folder = new Folder();
        
        if (node.has("links")) {
            for (JsonNode link : node.get("links")) {
                if ("self".equals(link.path("rel").asText())) {
                    folder.setPath(link.path("href").asText());
                }
            }
        }
        
        if (node.has("properties")) {
            JsonNode props = node.get("properties");
            folder.setName(props.path("name").asText());
            folder.setTitle(props.path("dc:title").asText(props.path("name").asText()));
        }
        
        if (folder.getName().isEmpty()) {
            folder.setName(node.path("name").asText());
        }
        
        return folder;
    }

    public static class Asset {
        private String path;
        private String name;
        private String title;
        private String mimeType;
        private String description;
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
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public String getCreated() { return created; }
        public void setCreated(String created) { this.created = created; }
        public String getModified() { return modified; }
        public void setModified(String modified) { this.modified = modified; }

        @Override
        public String toString() {
            return String.format("%s - %s (%s)", name, title, mimeType);
        }
    }

    public static class Folder {
        private String path;
        private String name;
        private String title;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        @Override
        public String toString() {
            return name + " (" + title + ")";
        }
    }
}
