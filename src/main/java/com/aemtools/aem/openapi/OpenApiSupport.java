package com.aemtools.aem.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads OpenAPI 3 specs (YAML or JSON, from a URL or local file) and
 * synthesizes example mock responses from the response schemas. Adobe hosts
 * the AEM as a Cloud Service OpenAPI specs on a Redocly registry, e.g.
 * {@code https://api.redocly.com/registry/bundle/adobe-developers/AEM-launches-author/stable/openapi.yaml?branch=prod}.
 */
public final class OpenApiSupport {

    private static final org.slf4j.Logger logger =
        LoggerFactory.getLogger(OpenApiSupport.class);

    private static final int MAX_DEPTH = 5;
    private static final String[] HTTP_METHODS =
        {"get", "post", "put", "delete", "patch", "head", "options"};

    private OpenApiSupport() {
    }

    /**
     * An operation extracted from an OpenAPI spec.
     */
    public record Operation(String method, String path, String operationId,
                            String summary, JsonNode node) {
        @Override
        public String toString() {
            return method.toUpperCase() + " " + path;
        }
    }

    /**
     * Loads a spec from an {@code http(s)://} URL or a local file path.
     *
     * @param source URL or file path
     * @return the parsed spec root
     * @throws IOException if fetching or parsing fails
     */
    public static JsonNode loadSpec(String source) throws IOException {
        String raw;
        if (source.startsWith("http://") || source.startsWith("https://")) {
            HttpURLConnection conn = (HttpURLConnection)
                URI.create(source).toURL().openConnection();
            conn.setRequestProperty("Accept",
                "application/yaml, application/json, text/plain, */*");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(30_000);
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Spec download failed: HTTP " + status + " for " + source);
            }
            try (InputStream in = conn.getInputStream()) {
                raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } finally {
                conn.disconnect();
            }
        } else {
            raw = Files.readString(Path.of(source), StandardCharsets.UTF_8);
        }

        com.fasterxml.jackson.dataformat.yaml.YAMLFactory yamlFactory =
            new com.fasterxml.jackson.dataformat.yaml.YAMLFactory();
        com.fasterxml.jackson.databind.ObjectMapper yamlMapper =
            new com.fasterxml.jackson.databind.ObjectMapper(yamlFactory);
        JsonNode spec = yamlMapper.readTree(raw);
        if (spec == null || spec.path("openapi").isMissingNode()) {
            throw new IOException("Not an OpenAPI spec (missing 'openapi' field): " + source);
        }
        return spec;
    }

    /**
     * Lists all operations in the spec.
     *
     * @param spec parsed OpenAPI root
     * @return one entry per method per path
     */
    public static List<Operation> operations(JsonNode spec) {
        List<Operation> ops = new ArrayList<>();
        JsonNode paths = spec.path("paths");
        paths.fields().forEachRemaining(pathEntry -> {
            String path = pathEntry.getKey();
            JsonNode pathItem = pathEntry.getValue();
            for (String method : HTTP_METHODS) {
                JsonNode opNode = pathItem.path(method);
                if (opNode.isObject()) {
                    String operationId = opNode.path("operationId").asText(null);
                    if (operationId == null) {
                        operationId = method.toUpperCase() + " " + path;
                    }
                    String summary = opNode.path("summary").asText(null);
                    ops.add(new Operation(method, path, operationId, summary, opNode));
                }
            }
        });
        return ops;
    }

    /**
     * Finds an operation by operationId or by {@code "METHOD /path"}.
     *
     * @param spec  parsed OpenAPI root
     * @param match operationId or "METHOD /path"
     * @return the matching operation, or null if not found
     */
    public static Operation findOperation(JsonNode spec, String match) {
        List<Operation> ops = operations(spec);
        for (Operation op : ops) {
            if (op.operationId().equalsIgnoreCase(match)) {
                return op;
            }
        }
        String upper = match.toUpperCase();
        for (Operation op : ops) {
            if (op.toString().equalsIgnoreCase(upper)) {
                return op;
            }
        }
        return null;
    }

    /**
     * Synthesizes a mock response body for an operation.
     *
     * @param spec   parsed OpenAPI root (for $ref resolution)
     * @param op     the operation
     * @param status desired HTTP status code; falls back to the first 2XX
     * @return the synthesized response, or null if the operation has no
     *         JSON body for that status
     */
    public static JsonNode mockResponse(JsonNode spec, Operation op, int status) {
        JsonNode responses = op.node().path("responses");
        JsonNode response = responses.path(String.valueOf(status));
        if (!response.isObject()) {
            // Fall back to the first 2XX response
            for (Map.Entry<String, JsonNode> entry : responses.properties()) {
                String key = entry.getKey();
                if (key.startsWith("2") || key.equalsIgnoreCase("default")) {
                    response = entry.getValue();
                    if (response.isObject()) {
                        break;
                    }
                }
            }
        }
        JsonNode content = response.path("content").path("application/json").path("schema");
        if (content.isMissingNode()) {
            return null;
        }
        return synthFromSchema(content, spec, new HashSet<>(), 0);
    }

    /**
     * Synthesizes an example value from a JSON schema node.
     *
     * @param schema  the schema node
     * @param root    the spec root, for $ref resolution
     * @param visited refs already being expanded (cycle guard)
     * @param depth   current recursion depth
     * @return synthesized value; empty object for unresolvable schemas
     */
    public static JsonNode synthFromSchema(JsonNode schema, JsonNode root,
                                           Set<String> visited, int depth) {
        com.fasterxml.jackson.databind.ObjectMapper mapper =
            new com.fasterxml.jackson.databind.ObjectMapper();

        if (schema == null || !schema.isObject() || depth > MAX_DEPTH) {
            return mapper.createObjectNode();
        }

        // Prefer explicit examples, then defaults
        JsonNode example = schema.get("example");
        if (example != null) {
            return example.deepCopy();
        }
        JsonNode examples = schema.path("examples");
        if (examples.isArray() && examples.size() > 0) {
            return examples.get(0).deepCopy();
        }
        if (examples.isObject() && examples.size() > 0) {
            JsonNode first = examples.properties().iterator().next().getValue();
            JsonNode value = first.path("value");
            if (!value.isMissingNode()) {
                return value.deepCopy();
            }
        }
        JsonNode def = schema.get("default");
        if (def != null) {
            return def.deepCopy();
        }

        // Resolve local $refs
        JsonNode ref = schema.get("$ref");
        if (ref != null && ref.isTextual()) {
            String refText = ref.asText();
            JsonNode resolved = resolveRef(refText, root);
            if (resolved == null) {
                logger.debug("Unresolvable $ref: {}", refText);
                return mapper.createObjectNode();
            }
            // The ref stays in the set: true cycles terminate immediately.
            // Shared non-cyclic refs are fine because callers pass a fresh
            // set per synthesized response.
            if (!visited.add(refText)) {
                return mapper.createObjectNode();
            }
            return synthFromSchema(resolved, root, visited, depth + 1);
        }

        JsonNode enumNode = schema.get("enum");
        if (enumNode != null && enumNode.isArray() && enumNode.size() > 0) {
            return enumNode.get(0).deepCopy();
        }

        String type = schema.path("type").asText(null);
        if (type == null) {
            JsonNode items = schema.get("items");
            if (items.isObject()) {
                return synthFromSchema(items, root, visited, depth);
            }
            return mapper.createObjectNode();
        }

        switch (type) {
            case "object": {
                com.fasterxml.jackson.databind.node.ObjectNode obj =
                    mapper.createObjectNode();
                JsonNode properties = schema.path("properties");
                if (properties.isObject()) {
                    properties.fields().forEachRemaining(prop -> {
                        JsonNode value = synthFromSchema(
                            prop.getValue(), root, visited, depth + 1);
                        obj.set(prop.getKey(), value);
                    });
                }
                return obj;
            }
            case "array": {
                com.fasterxml.jackson.databind.node.ArrayNode arr =
                    mapper.createArrayNode();
                JsonNode items = schema.path("items");
                if (items.isObject()) {
                    arr.add(synthFromSchema(items, root, visited, depth + 1));
                }
                return arr;
            }
            case "string": {
                return mapper.getNodeFactory().textNode(sampleString(schema));
            }
            case "integer": {
                return mapper.getNodeFactory().numberNode(0);
            }
            case "number": {
                return mapper.getNodeFactory().numberNode(0.0);
            }
            case "boolean": {
                return mapper.getNodeFactory().booleanNode(false);
            }
            default: {
                return mapper.createObjectNode();
            }
        }
    }

    /**
     * Resolves a local {@code #/components/schemas/Name} reference.
     *
     * @param ref  the reference string
     * @param root the spec root
     * @return the referenced node, or null if unresolvable
     */
    public static JsonNode resolveRef(String ref, JsonNode root) {
        if (!ref.startsWith("#/")) {
            return null;
        }
        JsonNode node = root;
        for (String part : ref.substring(2).split("/")) {
            node = node.path(part.replace("~1", "/").replace("~0", "~"));
            if (node.isMissingNode()) {
                return null;
            }
        }
        return node.isObject() ? node : null;
    }

    private static String sampleString(JsonNode schema) {
        String format = schema.path("format").asText("");
        return switch (format) {
            case "date" -> "2026-01-01";
            case "date-time" -> "2026-01-01T00:00:00Z";
            case "uuid" -> "00000000-0000-0000-0000-000000000000";
            case "uri" -> "https://example.com";
            case "email" -> "user@example.com";
            default -> "";
        };
    }

    /**
     * Convenience: builds a {@code name -> operation} index for display.
     *
     * @param spec parsed OpenAPI root
     * @return map of operation display string to operation
     */
    public static Map<String, Operation> operationIndex(JsonNode spec) {
        Map<String, Operation> index = new LinkedHashMap<>();
        for (Operation op : operations(spec)) {
            index.put(op.toString(), op);
        }
        return index;
    }
}
