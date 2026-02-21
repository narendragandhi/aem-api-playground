package com.example.aem.api;

import com.example.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PackagesApi {

    private final AemApiClient client;
    private final ObjectMapper mapper;

    public PackagesApi(AemApiClient client) {
        this.client = client;
        this.mapper = client.getObjectMapper();
    }

    public List<Package> list(String group) throws IOException {
        String path = group != null 
            ? "/crx/packmgr/list.jsp?filter=" + group + ":*" 
            : "/crx/packmgr/list.jsp";
        
        JsonNode response = client.get(path);
        List<Package> packages = new ArrayList<>();
        
        if (response.has("results")) {
            ArrayNode results = (ArrayNode) response.get("results");
            for (JsonNode pkgNode : results) {
                packages.add(parsePackage(pkgNode));
            }
        }
        
        return packages;
    }

    public Package get(String group, String name) throws IOException {
        String path = "/crx/packmgr/" + group + "/" + name + ".json";
        JsonNode response = client.get(path);
        return parsePackage(response);
    }

    public boolean build(String group, String name) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("cmd", "build");
        
        String path = "/crx/packmgr/service.jsp/" + group + "/" + name;
        JsonNode response = client.post(path, request);
        
        return response.has("success") && response.get("success").asBoolean();
    }

    public boolean install(String group, String name) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("cmd", "install");
        
        String path = "/crx/packmgr/service.jsp/" + group + "/" + name;
        JsonNode response = client.post(path, request);
        
        return response.has("success") && response.get("success").asBoolean();
    }

    public boolean uninstall(String group, String name) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("cmd", "uninstall");
        
        String path = "/crx/packmgr/service.jsp/" + group + "/" + name;
        JsonNode response = client.post(path, request);
        
        return response.has("success") && response.get("success").asBoolean();
    }

    public boolean delete(String group, String name) throws IOException {
        String path = "/crx/packmgr/" + group + "/" + name + ".json";
        return client.delete(path);
    }

    public Package upload(Path zipPath) throws IOException {
        byte[] data = java.nio.file.Files.readAllBytes(zipPath);
        String fileName = zipPath.getFileName().toString();
        
        return upload(data, fileName);
    }

    public Package upload(byte[] zipData, String fileName) throws IOException {
        String path = "/crx/packmgr/service.jsp?cmd=upload";
        
        JsonNode response = client.upload(path, zipData, "application/zip");
        
        if (response.has("success") && response.get("success").asBoolean()) {
            return parsePackage(response.path("package"));
        }
        
        throw new IOException("Upload failed: " + response.toString());
    }

    public boolean download(String group, String name, Path destPath) throws IOException {
        String path = "/crx/packmgr/" + group + "/" + name + ".zip";
        byte[] data = client.download(path);
        
        java.nio.file.Files.write(destPath, data);
        return true;
    }

    public boolean recreate(String group, String name, String filterXml) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("cmd", "recreate");
        request.put("filter", filterXml);
        
        String path = "/crx/packmgr/service.jsp/" + group + "/" + name;
        JsonNode response = client.post(path, request);
        
        return response.has("success") && response.get("success").asBoolean();
    }

    private Package parsePackage(JsonNode node) {
        Package pkg = new Package();
        
        String path = node.path("path").asText();
        String[] parts = path.split("/");
        if (parts.length >= 3) {
            pkg.setGroup(parts[1]);
            pkg.setName(parts[2].replace(".zip", ""));
        }
        
        pkg.setPath(path);
        pkg.setVersion(node.path("version").asText());
        pkg.setDescription(node.path("description").asText());
        pkg.setSize(node.path("size").asLong(0));
        pkg.setInstalled(node.path("installed").asBoolean(false));
        pkg.setBuilt(node.path("lastBuilt").asText() != null && !node.path("lastBuilt").asText().isEmpty());
        
        return pkg;
    }

    public static class Package {
        private String path;
        private String group;
        private String name;
        private String version;
        private String description;
        private long size;
        private boolean installed;
        private boolean built;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getGroup() { return group; }
        public void setGroup(String group) { this.group = group; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public boolean isInstalled() { return installed; }
        public void setInstalled(boolean installed) { this.installed = installed; }
        public boolean isBuilt() { return built; }
        public void setBuilt(boolean built) { this.built = built; }

        @Override
        public String toString() {
            return String.format("%s:%s:%s", group, name, version);
        }
    }
}
