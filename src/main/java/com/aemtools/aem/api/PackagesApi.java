package com.aemtools.aem.api;

import com.aemtools.aem.client.AemApiClient;
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
        String path = "/crx/packmgr/list.jsp?filter=" + encode(group) + ":" + encode(name);
        JsonNode response = client.get(path);

        if (response.has("results")) {
            ArrayNode results = (ArrayNode) response.get("results");
            for (JsonNode pkgNode : results) {
                Package pkg = parsePackage(pkgNode);
                if (group.equals(pkg.getGroup()) && name.equals(pkg.getName())) {
                    return pkg;
                }
            }
        }

        throw new IOException("Package not found: " + group + ":" + name);
    }

    public boolean build(String group, String name) throws IOException {
        AemApiClient.RawResponse response = runServiceCmd("build", group, name);
        return response.isSuccess();
    }

    public boolean install(String group, String name) throws IOException {
        AemApiClient.RawResponse response = runServiceCmd("inst", group, name);
        return response.isSuccess();
    }

    public boolean uninstall(String group, String name) throws IOException {
        AemApiClient.RawResponse response = runServiceCmd("uninst", group, name);
        return response.isSuccess();
    }

    public boolean delete(String group, String name) throws IOException {
        AemApiClient.RawResponse response = runServiceCmd("rm", group, name);
        return response.isSuccess();
    }

    public Package upload(Path zipPath) throws IOException {
        byte[] data = java.nio.file.Files.readAllBytes(zipPath);
        String fileName = zipPath.getFileName().toString();

        return upload(data, fileName);
    }

    public Package upload(byte[] zipData, String fileName) throws IOException {
        String path = "/crx/packmgr/service.jsp?cmd=upload";

        String responseBody = client.uploadMultipart(path, "file", fileName, zipData);

        String status = statusCode(responseBody);
        if (status.isEmpty() || !status.startsWith("2")) {
            String message = xmlTag(responseBody, "status");
            throw new IOException("Upload failed: " + (message.isEmpty() ? responseBody.trim() : message));
        }

        Package pkg = new Package();
        pkg.setGroup(xmlTag(responseBody, "group"));
        pkg.setName(xmlTag(responseBody, "name"));
        String version = xmlTag(responseBody, "version");
        pkg.setVersion(version);
        pkg.setSize(parseSize(xmlTag(responseBody, "size")));
        String group = pkg.getGroup().isEmpty() ? "temporary" : pkg.getGroup();
        pkg.setPath("/etc/packages/" + group + "/" + pkg.getName() + ".zip");
        return pkg;
    }

    public boolean download(String group, String name, Path destPath) throws IOException {
        String path = "/crx/packmgr/service.jsp?cmd=get&name=" + encode(name) + "&group=" + encode(group);
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

    private AemApiClient.RawResponse runServiceCmd(String cmd, String group, String name) throws IOException {
        String path = "/crx/packmgr/service.jsp?cmd=" + cmd
            + "&name=" + encode(name)
            + "&group=" + encode(group);
        return client.postRaw(path);
    }

    private static String encode(String value) throws IOException {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String xmlTag(String body, String tag) {
        java.util.regex.Matcher matcher =
            java.util.regex.Pattern.compile("<" + tag + "[^>]*>([^<]*)</" + tag + "[^>]*>").matcher(body);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private static String statusCode(String body) {
        java.util.regex.Matcher matcher =
            java.util.regex.Pattern.compile("<status[^>]*code=\"(\\d+)\"").matcher(body);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static long parseSize(String value) {
        try {
            return value.isEmpty() ? 0 : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Package parsePackage(JsonNode node) {
        Package pkg = new Package();

        String path = node.path("path").asText();
        String name = node.path("name").asText();
        String group = node.path("group").asText();
        if (name.isEmpty() || group.isEmpty()) {
            String[] parts = path.replace(".zip", "").split("/");
            if (parts.length >= 3) {
                if (group.isEmpty()) {
                    group = parts[parts.length - 2];
                }
                if (name.isEmpty()) {
                    name = parts[parts.length - 1];
                }
            }
        }
        pkg.setPath(path);
        pkg.setGroup(group);
        pkg.setName(name);
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
