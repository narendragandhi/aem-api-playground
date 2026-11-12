package com.aemtools.aem;

import com.aemtools.aem.api.JcrApi;
import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JcrApiMockTest {

    @Mock
    private AemApiClient mockClient;

    private JcrApi jcrApi;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        jcrApi = new JcrApi(mockClient);
    }

    @Test
    void testGetNodeBuildsDepthRenderer() throws IOException {
        ObjectNode node = mapper.createObjectNode();
        node.put("jcr:title", "Hello");
        when(mockClient.get("/content/we-retail.2.json")).thenReturn(node);

        assertEquals("Hello", jcrApi.getNode("/content/we-retail", 2).path("jcr:title").asText());
    }

    @Test
    void testGetNodeSupportsFullDepth() throws IOException {
        when(mockClient.get("/content.-1.json")).thenReturn(mapper.createObjectNode());

        jcrApi.getNode("/content", -1);

        verify(mockClient).get("/content.-1.json");
    }

    @Test
    void testSaveNodePostsSlingOperatorsVerbatim() throws IOException {
        when(mockClient.post(anyString(), any())).thenReturn(mapper.createObjectNode());

        jcrApi.saveNode("/content/page", Map.of(
            "jcr:title", "New title",
            "cq:tags@TypeHint", "String[]",
            "cq:tags@Delete", ""));

        verify(mockClient).post(eq("/content/page"), eq(Map.of(
            "jcr:title", "New title",
            "cq:tags@TypeHint", "String[]",
            "cq:tags@Delete", "")));
    }

    @Test
    void testSaveNodeRejectsEmptyProps() throws IOException {
        assertThrows(IllegalArgumentException.class,
            () -> jcrApi.saveNode("/content/page", Map.of()));
        assertThrows(IllegalArgumentException.class,
            () -> jcrApi.saveNode("/content/page", null));
        verify(mockClient, never()).post(anyString(), any());
    }

    @Test
    void testDeleteNode() throws IOException {
        when(mockClient.delete("/content/old")).thenReturn(true);

        assertTrue(jcrApi.deleteNode("/content/old"));
        verify(mockClient).delete("/content/old");
    }

    @Test
    void testCopyAndMove() throws IOException {
        when(mockClient.copy("/content/a", "/content/b")).thenReturn(mapper.createObjectNode());
        when(mockClient.move("/content/a", "/content/c")).thenReturn(mapper.createObjectNode());

        jcrApi.copyNode("/content/a", "/content/b");
        jcrApi.moveNode("/content/a", "/content/c");

        verify(mockClient).copy("/content/a", "/content/b");
        verify(mockClient).move("/content/a", "/content/c");
    }
}
