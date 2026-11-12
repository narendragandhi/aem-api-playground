package com.aemtools.aem.api;

import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * API client for AEM content search via the Query Builder servlet
 * ({@code /bin/querybuilder.json}) — Adobe's documented content-search API.
 */
public class QueryApi {

    private static final Logger logger = LoggerFactory.getLogger(QueryApi.class);
    private final AemApiClient client;

    public QueryApi(AemApiClient client) {
        this.client = client;
    }

    /**
     * Runs a generic Query Builder search.
     *
     * @param path     content path to search under (default /content)
     * @param type     primary node type filter (e.g. cq:Page, dam:Asset, nt:file), may be null
     * @param params   extra Query Builder parameters as key=value pairs
     *                 (e.g. "property=cq:tags", "property.value=geo:us",
     *                 "fulltext=hello", "1_property=jcr:title")
     * @param limit    maximum number of hits (0 = unbounded with guessTotal)
     * @return the raw Query Builder response
     * @throws IOException if the API call fails
     */
    public JsonNode search(String path, String type, List<String> params, int limit) throws IOException {
        String queryPath = buildQueryPath(path, type, params, limit);
        JsonNode response = client.get(queryPath);
        logger.info("Query returned {} total", response.path("total").asInt(-1));
        return response;
    }

    /**
     * Builds the Query Builder query string. Pure and public for testing.
     *
     * @param path   content path to search under
     * @param type   primary node type filter, may be null
     * @param params extra parameters as key=value pairs
     * @param limit  maximum number of hits
     * @return the query string, e.g. {@code /bin/querybuilder.json?path=...}
     */
    public static String buildQueryPath(String path, String type, List<String> params, int limit) {
        List<String> parts = new ArrayList<>();
        parts.add("path=" + encode(path != null ? path : "/content"));
        if (type != null && !type.isBlank()) {
            parts.add("type=" + encode(type));
        }
        parts.add("p.limit=" + limit);
        if (limit <= 0) {
            parts.add("p.guessTotal=true");
        }
        if (params != null) {
            for (String param : params) {
                if (param == null || param.isBlank()) {
                    continue;
                }
                int eq = param.indexOf('=');
                if (eq <= 0) {
                    throw new IllegalArgumentException(
                        "Query parameters must be key=value pairs: " + param);
                }
                String key = param.substring(0, eq);
                String value = param.substring(eq + 1);
                if (key.contains(" ")) {
                    throw new IllegalArgumentException("Invalid parameter key: " + key);
                }
                parts.add(encode(key) + "=" + encode(value));
            }
        }
        parts.add("p.hits=full");
        return "/bin/querybuilder.json?" + String.join("&", parts);
    }

    /**
     * Extracts the hit paths from a Query Builder response.
     *
     * @param response the raw response
     * @return list of jcr:path values from the hits array
     */
    public static List<String> hitPaths(JsonNode response) {
        List<String> paths = new ArrayList<>();
        JsonNode hits = response.path("hits");
        if (hits.isArray()) {
            for (JsonNode hit : hits) {
                String path = hit.path("jcr:path").asText(hit.path("path").asText(null));
                if (path != null && !path.isBlank()) {
                    paths.add(path);
                }
            }
        }
        return paths;
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot encode query parameter: " + value, e);
        }
    }
}
