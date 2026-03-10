package com.aemtools.aem.api;

import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API client for AEM Tag management operations.
 * Supports creating, listing, updating, and deleting tags (taxonomy management).
 */
public class TagsApi {

    private static final Logger logger = LoggerFactory.getLogger(TagsApi.class);
    private static final String TAG_BASE_PATH = "/content/cq:tags";
    private final AemApiClient client;

    public TagsApi(AemApiClient client) {
        this.client = client;
    }

    // ==================== Tag Operations ====================

    /**
     * Lists tags in a namespace or path.
     *
     * @param path the tag path (default: /content/cq:tags)
     * @param recursive whether to list tags recursively
     * @param limit maximum number of tags to return
     * @return list of tags
     * @throws IOException if the API call fails
     */
    public List<Tag> listTags(String path, boolean recursive, int limit) throws IOException {
        String searchPath = path != null ? path : TAG_BASE_PATH;

        String queryPath;
        if (recursive) {
            queryPath = String.format(
                "/bin/querybuilder.json?path=%s&type=cq:Tag&p.limit=%d&p.hits=full&orderby=@jcr:path",
                URLEncoder.encode(searchPath, StandardCharsets.UTF_8), limit
            );
        } else {
            // Non-recursive: just get direct children
            queryPath = searchPath + ".1.json";
        }

        JsonNode response = client.get(queryPath);
        List<Tag> tags = new ArrayList<>();

        if (recursive) {
            JsonNode hits = response.path("hits");
            if (hits.isArray()) {
                for (JsonNode hit : hits) {
                    tags.add(parseTag(hit));
                }
            }
        } else {
            // Parse direct JSON children
            response.fields().forEachRemaining(entry -> {
                String name = entry.getKey();
                JsonNode node = entry.getValue();
                if (node.isObject() && !name.startsWith("jcr:") && !name.startsWith("cq:")) {
                    tags.add(parseTagFromNode(searchPath + "/" + name, node));
                }
            });
        }

        logger.info("Found {} tags in {}", tags.size(), searchPath);
        return tags;
    }

    /**
     * Lists all tag namespaces.
     *
     * @return list of tag namespaces
     * @throws IOException if the API call fails
     */
    public List<TagNamespace> listNamespaces() throws IOException {
        JsonNode response = client.get(TAG_BASE_PATH + ".1.json");
        List<TagNamespace> namespaces = new ArrayList<>();

        response.fields().forEachRemaining(entry -> {
            String name = entry.getKey();
            JsonNode node = entry.getValue();
            if (node.isObject() && !name.startsWith("jcr:") && !name.startsWith("cq:") && !name.startsWith("rep:")) {
                String title = node.path("jcr:title").asText(name);
                String description = node.path("jcr:description").asText(null);
                String path = TAG_BASE_PATH + "/" + name;

                // Count child tags
                int tagCount = 0;
                var fields = node.fields();
                while (fields.hasNext()) {
                    var field = fields.next();
                    if (field.getValue().isObject() && !field.getKey().startsWith("jcr:")) {
                        tagCount++;
                    }
                }

                namespaces.add(new TagNamespace(name, path, title, description, tagCount));
            }
        });

        logger.info("Found {} tag namespaces", namespaces.size());
        return namespaces;
    }

    /**
     * Gets a specific tag by its tag ID.
     *
     * @param tagId the tag ID (e.g., "namespace:tag/subtag")
     * @return the tag details
     * @throws IOException if the API call fails
     */
    public Tag getTag(String tagId) throws IOException {
        String path = tagIdToPath(tagId);
        JsonNode response = client.get(path + ".json");
        return parseTagFromNode(path, response);
    }

    /**
     * Creates a new tag.
     *
     * @param tagId the tag ID (e.g., "namespace:tag" or "namespace:parent/child")
     * @param title the display title
     * @param description optional description
     * @return the created tag
     * @throws IOException if the API call fails
     */
    public Tag createTag(String tagId, String title, String description) throws IOException {
        // Parse the tag ID to get namespace and tag name
        String[] parts = tagId.split(":", 2);
        String namespace = parts[0];
        String tagName = parts.length > 1 ? parts[1] : parts[0];

        // Build the path
        String path = TAG_BASE_PATH + "/" + namespace;
        if (parts.length > 1) {
            path = TAG_BASE_PATH + "/" + namespace + "/" + tagName.replace("/", "/");
        }

        // Create parent tags if needed
        String[] tagParts = tagName.split("/");
        String currentPath = TAG_BASE_PATH + "/" + namespace;

        for (int i = 0; i < tagParts.length; i++) {
            String tagPart = tagParts[i];
            String tagPath = currentPath + "/" + tagPart;

            // Check if this is the final tag
            boolean isFinal = (i == tagParts.length - 1);

            Map<String, Object> params = new HashMap<>();
            params.put("jcr:primaryType", "cq:Tag");
            params.put("jcr:title", isFinal ? title : tagPart);
            if (isFinal && description != null) {
                params.put("jcr:description", description);
            }

            try {
                client.post(tagPath, params);
            } catch (IOException e) {
                // Tag might already exist, continue
                if (!e.getMessage().contains("409") && !e.getMessage().contains("already exists")) {
                    throw e;
                }
            }

            currentPath = tagPath;
        }

        logger.info("Created tag: {}", tagId);
        return getTag(tagId);
    }

    /**
     * Creates a new tag namespace.
     *
     * @param namespaceId the namespace ID (short name)
     * @param title the display title
     * @param description optional description
     * @return the created namespace
     * @throws IOException if the API call fails
     */
    public TagNamespace createNamespace(String namespaceId, String title, String description) throws IOException {
        String path = TAG_BASE_PATH + "/" + namespaceId;

        Map<String, Object> params = new HashMap<>();
        params.put("jcr:primaryType", "cq:Tag");
        params.put("jcr:title", title != null ? title : namespaceId);
        if (description != null) {
            params.put("jcr:description", description);
        }

        client.post(path, params);

        logger.info("Created namespace: {}", namespaceId);

        return new TagNamespace(namespaceId, path, title != null ? title : namespaceId, description, 0);
    }

    /**
     * Updates a tag's properties.
     *
     * @param tagId the tag ID
     * @param title new title (null to keep existing)
     * @param description new description (null to keep existing)
     * @return the updated tag
     * @throws IOException if the API call fails
     */
    public Tag updateTag(String tagId, String title, String description) throws IOException {
        String path = tagIdToPath(tagId);

        Map<String, Object> params = new HashMap<>();
        if (title != null) {
            params.put("jcr:title", title);
        }
        if (description != null) {
            params.put("jcr:description", description);
        }

        client.post(path, params);

        logger.info("Updated tag: {}", tagId);
        return getTag(tagId);
    }

    /**
     * Deletes a tag.
     *
     * @param tagId the tag ID to delete
     * @param force whether to force delete even if tag is in use
     * @return true if deleted successfully
     * @throws IOException if the API call fails
     */
    public boolean deleteTag(String tagId, boolean force) throws IOException {
        String path = tagIdToPath(tagId);

        // Check if tag is in use
        if (!force) {
            int usage = getTagUsageCount(tagId);
            if (usage > 0) {
                throw new IOException("Tag is in use by " + usage + " items. Use force=true to delete anyway.");
            }
        }

        boolean result = client.delete(path);

        if (result) {
            logger.info("Deleted tag: {}", tagId);
        }

        return result;
    }

    /**
     * Moves/renames a tag.
     *
     * @param sourceTagId the current tag ID
     * @param destTagId the new tag ID
     * @return the moved tag
     * @throws IOException if the API call fails
     */
    public Tag moveTag(String sourceTagId, String destTagId) throws IOException {
        String sourcePath = tagIdToPath(sourceTagId);
        String destPath = tagIdToPath(destTagId);

        client.move(sourcePath, destPath);

        logger.info("Moved tag {} to {}", sourceTagId, destTagId);
        return getTag(destTagId);
    }

    /**
     * Merges one tag into another.
     * All content tagged with sourceTag will be retagged with destTag.
     *
     * @param sourceTagId the tag to merge from (will be deleted)
     * @param destTagId the tag to merge into
     * @return number of items retagged
     * @throws IOException if the API call fails
     */
    public int mergeTags(String sourceTagId, String destTagId) throws IOException {
        // Find all content using the source tag
        List<String> contentPaths = getTaggedContent(sourceTagId, 1000);

        // Retag each piece of content
        int retagged = 0;
        for (String contentPath : contentPaths) {
            try {
                // Get current tags
                JsonNode content = client.get(contentPath + ".json");
                JsonNode tagsNode = content.path("cq:tags");

                List<String> tags = new ArrayList<>();
                if (tagsNode.isArray()) {
                    for (JsonNode tag : tagsNode) {
                        String tagValue = tag.asText();
                        if (tagValue.equals(sourceTagId)) {
                            tags.add(destTagId);
                        } else {
                            tags.add(tagValue);
                        }
                    }
                }

                // Update tags
                Map<String, Object> params = new HashMap<>();
                params.put("cq:tags", tags.toArray(new String[0]));
                params.put("cq:tags@TypeHint", "String[]");

                client.post(contentPath, params);
                retagged++;
            } catch (Exception e) {
                logger.warn("Failed to retag {}: {}", contentPath, e.getMessage());
            }
        }

        // Delete the source tag
        deleteTag(sourceTagId, true);

        logger.info("Merged tag {} into {}, retagged {} items", sourceTagId, destTagId, retagged);
        return retagged;
    }

    // ==================== Tag Usage ====================

    /**
     * Gets the number of items using a tag.
     *
     * @param tagId the tag ID
     * @return usage count
     * @throws IOException if the API call fails
     */
    public int getTagUsageCount(String tagId) throws IOException {
        String queryPath = String.format(
            "/bin/querybuilder.json?property=cq:tags&property.value=%s&p.limit=0&p.guessTotal=true",
            URLEncoder.encode(tagId, StandardCharsets.UTF_8)
        );

        JsonNode response = client.get(queryPath);
        return response.path("total").asInt(0);
    }

    /**
     * Gets content that uses a specific tag.
     *
     * @param tagId the tag ID
     * @param limit maximum results
     * @return list of content paths
     * @throws IOException if the API call fails
     */
    public List<String> getTaggedContent(String tagId, int limit) throws IOException {
        String queryPath = String.format(
            "/bin/querybuilder.json?property=cq:tags&property.value=%s&p.limit=%d",
            URLEncoder.encode(tagId, StandardCharsets.UTF_8), limit
        );

        JsonNode response = client.get(queryPath);
        List<String> paths = new ArrayList<>();

        JsonNode hits = response.path("hits");
        if (hits.isArray()) {
            for (JsonNode hit : hits) {
                paths.add(hit.path("jcr:path").asText(hit.path("path").asText()));
            }
        }

        return paths;
    }

    /**
     * Applies tags to content.
     *
     * @param contentPath the content path
     * @param tagIds list of tag IDs to apply
     * @param replace whether to replace existing tags or append
     * @return true if successful
     * @throws IOException if the API call fails
     */
    public boolean applyTags(String contentPath, List<String> tagIds, boolean replace) throws IOException {
        List<String> finalTags = new ArrayList<>();

        if (!replace) {
            // Get existing tags
            try {
                JsonNode content = client.get(contentPath + ".json");
                JsonNode tagsNode = content.path("cq:tags");
                if (tagsNode.isArray()) {
                    for (JsonNode tag : tagsNode) {
                        String tagValue = tag.asText();
                        if (!finalTags.contains(tagValue)) {
                            finalTags.add(tagValue);
                        }
                    }
                }
            } catch (Exception e) {
                // Content might not exist yet
            }
        }

        // Add new tags
        for (String tagId : tagIds) {
            if (!finalTags.contains(tagId)) {
                finalTags.add(tagId);
            }
        }

        // Apply tags
        Map<String, Object> params = new HashMap<>();
        params.put("cq:tags", finalTags.toArray(new String[0]));
        params.put("cq:tags@TypeHint", "String[]");

        client.post(contentPath, params);

        logger.info("Applied {} tags to {}", finalTags.size(), contentPath);
        return true;
    }

    /**
     * Removes tags from content.
     *
     * @param contentPath the content path
     * @param tagIds list of tag IDs to remove
     * @return true if successful
     * @throws IOException if the API call fails
     */
    public boolean removeTags(String contentPath, List<String> tagIds) throws IOException {
        // Get existing tags
        JsonNode content = client.get(contentPath + ".json");
        JsonNode tagsNode = content.path("cq:tags");

        List<String> remainingTags = new ArrayList<>();
        if (tagsNode.isArray()) {
            for (JsonNode tag : tagsNode) {
                String tagValue = tag.asText();
                if (!tagIds.contains(tagValue)) {
                    remainingTags.add(tagValue);
                }
            }
        }

        // Update tags
        Map<String, Object> params = new HashMap<>();
        if (remainingTags.isEmpty()) {
            params.put("cq:tags@Delete", "");
        } else {
            params.put("cq:tags", remainingTags.toArray(new String[0]));
            params.put("cq:tags@TypeHint", "String[]");
        }

        client.post(contentPath, params);

        logger.info("Removed {} tags from {}", tagIds.size(), contentPath);
        return true;
    }

    // ==================== Localization ====================

    /**
     * Sets a localized title for a tag.
     *
     * @param tagId the tag ID
     * @param locale the locale (e.g., "de", "fr", "ja")
     * @param title the localized title
     * @return the updated tag
     * @throws IOException if the API call fails
     */
    public Tag setLocalizedTitle(String tagId, String locale, String title) throws IOException {
        String path = tagIdToPath(tagId);

        Map<String, Object> params = new HashMap<>();
        params.put("jcr:title." + locale, title);

        client.post(path, params);

        logger.info("Set {} title for tag {}: {}", locale, tagId, title);
        return getTag(tagId);
    }

    /**
     * Gets all localized titles for a tag.
     *
     * @param tagId the tag ID
     * @return map of locale to title
     * @throws IOException if the API call fails
     */
    public Map<String, String> getLocalizedTitles(String tagId) throws IOException {
        String path = tagIdToPath(tagId);
        JsonNode response = client.get(path + ".json");

        Map<String, String> titles = new HashMap<>();
        response.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            if (key.startsWith("jcr:title.")) {
                String locale = key.substring("jcr:title.".length());
                titles.put(locale, entry.getValue().asText());
            }
        });

        // Include default title
        titles.put("default", response.path("jcr:title").asText());

        return titles;
    }

    // ==================== Helper Methods ====================

    private String tagIdToPath(String tagId) {
        // Convert tag ID to path
        // e.g., "namespace:tag/subtag" -> "/content/cq:tags/namespace/tag/subtag"
        String[] parts = tagId.split(":", 2);
        if (parts.length == 2) {
            return TAG_BASE_PATH + "/" + parts[0] + "/" + parts[1].replace("/", "/");
        }
        return TAG_BASE_PATH + "/" + tagId;
    }

    private String pathToTagId(String path) {
        // Convert path to tag ID
        // e.g., "/content/cq:tags/namespace/tag/subtag" -> "namespace:tag/subtag"
        String relativePath = path.replace(TAG_BASE_PATH + "/", "");
        int slashIndex = relativePath.indexOf('/');
        if (slashIndex > 0) {
            String namespace = relativePath.substring(0, slashIndex);
            String tagPath = relativePath.substring(slashIndex + 1);
            return namespace + ":" + tagPath;
        }
        return relativePath;
    }

    private Tag parseTag(JsonNode node) {
        String path = node.path("jcr:path").asText(node.path("path").asText());
        return parseTagFromNode(path, node);
    }

    private Tag parseTagFromNode(String path, JsonNode node) {
        String tagId = pathToTagId(path);
        String title = node.path("jcr:title").asText(path.substring(path.lastIndexOf('/') + 1));
        String description = node.path("jcr:description").asText(null);

        // Count child tags
        int childCount = 0;
        var fields = node.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            if (field.getValue().isObject() && !field.getKey().startsWith("jcr:") && !field.getKey().startsWith("cq:")) {
                childCount++;
            }
        }

        // Extract namespace
        String[] parts = tagId.split(":", 2);
        String namespace = parts[0];

        return new Tag(tagId, path, title, description, namespace, childCount);
    }

    // ==================== Data Classes ====================

    /**
     * Represents an AEM tag.
     */
    public record Tag(
        String tagId,
        String path,
        String title,
        String description,
        String namespace,
        int childCount
    ) {
        @Override
        public String toString() {
            return String.format("Tag[%s, %s%s]",
                tagId,
                title,
                childCount > 0 ? " (" + childCount + " children)" : "");
        }
    }

    /**
     * Represents an AEM tag namespace.
     */
    public record TagNamespace(
        String id,
        String path,
        String title,
        String description,
        int tagCount
    ) {
        @Override
        public String toString() {
            return String.format("Namespace[%s, %s, %d tags]", id, title, tagCount);
        }
    }
}
