package com.aemtools.aem;

import com.aemtools.aem.api.QueryApi;
import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryApiMockTest {

    @Mock
    private AemApiClient mockClient;

    private QueryApi queryApi;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        queryApi = new QueryApi(mockClient);
    }

    private ObjectNode responseWithHits(String... paths) {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode hits = mapper.createArrayNode();
        for (String path : paths) {
            ObjectNode hit = mapper.createObjectNode();
            hit.put("jcr:path", path);
            hits.add(hit);
        }
        response.set("hits", hits);
        response.put("total", paths.length);
        return response;
    }

    @Test
    void testSearchWithDefaultsBuildsCorrectQuery() throws IOException {
        when(mockClient.get(contains("/bin/querybuilder.json"))).thenReturn(responseWithHits("/content/a"));

        JsonNode response = queryApi.search(null, null, null, 20);

        assertEquals("/content/a", QueryApi.hitPaths(response).get(0));
        verify(mockClient).get(contains("/bin/querybuilder.json?path=%2Fcontent&p.limit=20&p.hits=full"));
    }

    @Test
    void testSearchWithTypeAndParams() throws IOException {
        when(mockClient.get(contains("/bin/querybuilder.json"))).thenReturn(responseWithHits());

        queryApi.search("/content/we-retail", "dam:Asset",
            List.of("property=cq:tags", "property.value=geo:us"), 0);

        verify(mockClient).get(contains("path=%2Fcontent%2Fwe-retail&type=dam%3AAsset"));
        verify(mockClient).get(contains("property=cq%3Atags&property.value=geo%3Aus"));
        verify(mockClient).get(contains("p.limit=0&p.guessTotal=true"));
    }

    @Test
    void testBuildQueryPathIsPureAndEncoded() {
        String query = QueryApi.buildQueryPath("/content/dam my", "cq:Page",
            List.of("fulltext=hello world"), 10);

        assertEquals("/bin/querybuilder.json?path=%2Fcontent%2Fdam+my&type=cq%3APage"
            + "&p.limit=10&fulltext=hello+world&p.hits=full", query);
    }

    @Test
    void testBuildQueryPathRejectsInvalidParams() {
        assertThrows(IllegalArgumentException.class,
            () -> QueryApi.buildQueryPath("/content", null, List.of("novalue"), 10));
        assertThrows(IllegalArgumentException.class,
            () -> QueryApi.buildQueryPath("/content", null, List.of("key with space=v"), 10));
        assertThrows(IllegalArgumentException.class,
            () -> QueryApi.buildQueryPath("/content", null, List.of("=value"), 10));
    }

    @Test
    void testHitPathsSkipsMissingPath() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode hits = mapper.createArrayNode();
        ObjectNode hit = mapper.createObjectNode();
        hit.put("jcr:path", "/content/has-path");
        hits.add(hit);
        hits.add(mapper.createObjectNode());
        response.set("hits", hits);

        assertEquals(List.of("/content/has-path"), QueryApi.hitPaths(response));
    }
}
