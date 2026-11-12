package com.aemtools.aem.api;

import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * API client for generic JCR node operations via the Sling POST servlet and
 * the node JSON renderers — Adobe's documented way to read and update pages
 * and arbitrary nodes (see the Sling Post Servlet documentation on the
 * Adobe developer portal).
 */
public class JcrApi {

    private static final Logger logger = LoggerFactory.getLogger(JcrApi.class);
    private final AemApiClient client;

    public JcrApi(AemApiClient client) {
        this.client = client;
    }

    /**
     * Gets a node's JSON representation.
     *
     * @param path  the node path
     * @param depth recursion depth (-1 = all levels, 0 = node only, n = n levels)
     * @return the node JSON
     * @throws IOException if the API call fails
     */
    public JsonNode getNode(String path, int depth) throws IOException {
        return client.get(path + "." + depth + ".json");
    }

    /**
     * Saves properties on a node via the Sling POST servlet. Sling operators
     * are passed through verbatim in property keys, e.g. {@code cq:tags@TypeHint=String[]}
     * or {@code cq:tags@Delete}.
     *
     * @param path  the node path
     * @param props property keys and values (may include Sling operators in keys)
     * @return the Sling response
     * @throws IOException if the API call fails
     */
    public JsonNode saveNode(String path, Map<String, Object> props) throws IOException {
        if (props == null || props.isEmpty()) {
            throw new IllegalArgumentException("No properties to save");
        }
        JsonNode response = client.post(path, props);
        logger.info("Saved {} properties on {}", props.size(), path);
        return response;
    }

    /**
     * Deletes a node.
     *
     * @param path the node path
     * @return true if deleted
     * @throws IOException if the API call fails
     */
    public boolean deleteNode(String path) throws IOException {
        boolean result = client.delete(path);
        if (result) {
            logger.info("Deleted node {}", path);
        }
        return result;
    }

    /**
     * Copies a node subtree to a new location.
     *
     * @param source source node path
     * @param dest   destination path
     * @return the Sling response
     * @throws IOException if the API call fails
     */
    public JsonNode copyNode(String source, String dest) throws IOException {
        return client.copy(source, dest);
    }

    /**
     * Moves (renames) a node subtree.
     *
     * @param source source node path
     * @param dest   destination path
     * @return the Sling response
     * @throws IOException if the API call fails
     */
    public JsonNode moveNode(String source, String dest) throws IOException {
        return client.move(source, dest);
    }
}
