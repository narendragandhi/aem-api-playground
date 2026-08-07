package com.aemtools.aem;

import com.aemtools.aem.api.PagesApi;
import com.aemtools.aem.api.PagesApi.Page;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagesApiMockTest {

    @Mock
    private AemApiClient mockClient;

    private PagesApi pagesApi;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        when(mockClient.getObjectMapper()).thenReturn(mapper);
        pagesApi = new PagesApi(mockClient);
    }

    @Test
    void testListParsesDataArray() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode data = mapper.createArrayNode();
        ObjectNode page = mapper.createObjectNode();
        page.put("path", "/content/mysite");
        page.put("name", "mysite");
        page.put("jcr:title", "My Site");
        page.put("cq:template", "/conf/templates/content");
        data.add(page);
        response.set("data", data);

        when(mockClient.get(contains("type=cq:Page"))).thenReturn(response);

        List<Page> pages = pagesApi.list("/content", 10);

        assertEquals(1, pages.size());
        assertEquals("/content/mysite", pages.get(0).getPath());
        assertEquals("My Site", pages.get(0).getTitle());
    }

    @Test
    void testListReturnsEmptyWhenNoData() throws IOException {
        when(mockClient.get(anyString())).thenReturn(mapper.createObjectNode());

        List<Page> pages = pagesApi.list("/content", 10);

        assertTrue(pages.isEmpty());
    }

    @Test
    void testGetUsesJcrPathFallback() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jcr:path", "/content/mysite/home");
        response.put("jcr:title", "Home");

        when(mockClient.get(eq("/content/mysite/home.json"))).thenReturn(response);

        Page page = pagesApi.get("/content/mysite/home");

        assertEquals("/content/mysite/home", page.getPath());
        assertEquals("Home", page.getTitle());
    }

    @Test
    void testGetContentDetectsRoot() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jcr:title", "About");
        response.put("root", true);

        when(mockClient.get(eq("/content/mysite/about/jcr:content.json"))).thenReturn(response);

        Page page = pagesApi.getContent("/content/mysite/about");

        assertEquals("/content/mysite/about", page.getPath());
        assertEquals("About", page.getTitle());
        assertTrue(page.isHasContent());
    }

    @Test
    void testCreatePostsToApiPages() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("path", "/content/mysite/home");
        response.put("name", "home");
        response.put("title", "Home");

        when(mockClient.post(anyString(), any())).thenReturn(response);

        Page page = pagesApi.create("/content/mysite", "home", "/conf/t", "Home");

        assertEquals("/content/mysite/home", page.getPath());
        verify(mockClient).post(eq("/api/pages/content/mysite/home"), any());
    }

    @Test
    void testDeleteCallsApiPages() throws IOException {
        when(mockClient.delete(eq("/api/pages/content/mysite/home"))).thenReturn(true);

        assertTrue(pagesApi.delete("/content/mysite/home"));
        verify(mockClient).delete(eq("/api/pages/content/mysite/home"));
    }

    @Test
    void testUpdateCallsPut() throws IOException {
        ObjectNode content = mapper.createObjectNode();
        content.put("jcr:title", "Updated");

        ObjectNode response = mapper.createObjectNode();
        response.put("jcr:title", "Updated");

        when(mockClient.put(anyString(), any())).thenReturn(response);

        JsonNode result = pagesApi.update("/content/mysite/home", content);

        assertEquals("Updated", result.path("jcr:title").asText());
        verify(mockClient).put(eq("/api/pages/content/mysite/home/jcr:content.json"), eq(content));
    }

    @Test
    void testMovePostsDest() throws IOException {
        when(mockClient.post(anyString(), any())).thenReturn(mapper.createObjectNode());

        pagesApi.move("/content/mysite/home", "/content/other/home");

        verify(mockClient).post(eq("/api/pages/content/mysite/home.move.json"), any());
    }

    @Test
    void testSearchParsesHits() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode hits = mapper.createArrayNode();
        ObjectNode hit = mapper.createObjectNode();
        hit.put("path", "/content/mysite");
        hit.put("jcr:title", "My Site");
        hits.add(hit);
        response.set("hits", hits);

        when(mockClient.post(contains("/bin/cq/search.json"), any())).thenReturn(response);

        List<Page> pages = pagesApi.search("mysite", 5);

        assertEquals(1, pages.size());
        assertEquals("My Site", pages.get(0).getTitle());
    }

    @Test
    void testSearchReturnsEmptyWhenNoHits() throws IOException {
        when(mockClient.post(contains("/bin/cq/search.json"), any()))
            .thenReturn(mapper.createObjectNode());

        assertTrue(pagesApi.search("nope", 5).isEmpty());
    }
}
