package com.aemtools.aem;

import com.aemtools.aem.api.TagsApi;
import com.aemtools.aem.api.TagsApi.Tag;
import com.aemtools.aem.api.TagsApi.TagNamespace;
import com.aemtools.aem.client.AemApiClient;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagsApiMockTest {

    @Mock
    private AemApiClient mockClient;

    private TagsApi tagsApi;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        tagsApi = new TagsApi(mockClient);
    }

    @Test
    void testListTagsRecursiveParsesHits() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode hits = mapper.createArrayNode();
        ObjectNode hit = mapper.createObjectNode();
        hit.put("jcr:path", "/content/cq:tags/marketing/campaigns/summer");
        hit.put("jcr:title", "Summer Campaign");
        hits.add(hit);
        response.set("hits", hits);

        when(mockClient.get(contains("querybuilder"))).thenReturn(response);

        List<Tag> tags = tagsApi.listTags("/content/cq:tags/marketing", true, 10);

        assertEquals(1, tags.size());
        assertEquals("marketing:campaigns/summer", tags.get(0).tagId());
        assertEquals("Summer Campaign", tags.get(0).title());
    }

    @Test
    void testListTagsNonRecursiveParsesChildren() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jcr:primaryType", "cq:Tag");
        ObjectNode child = mapper.createObjectNode();
        child.put("jcr:primaryType", "cq:Tag");
        child.put("jcr:title", "Electronics");
        response.set("electronics", child);

        when(mockClient.get(contains(".1.json"))).thenReturn(response);

        List<Tag> tags = tagsApi.listTags("/content/cq:tags/products", false, 10);

        assertEquals(1, tags.size());
        assertEquals("products:electronics", tags.get(0).tagId());
    }

    @Test
    void testListNamespaces() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jcr:primaryType", "cq:Tag");
        ObjectNode ns = mapper.createObjectNode();
        ns.put("jcr:primaryType", "cq:Tag");
        ns.put("jcr:title", "Marketing");
        ObjectNode child = mapper.createObjectNode();
        child.put("jcr:primaryType", "cq:Tag");
        ns.set("campaigns", child);
        response.set("marketing", ns);

        when(mockClient.get(contains("/content/cq:tags.1.json"))).thenReturn(response);

        List<TagNamespace> namespaces = tagsApi.listNamespaces();

        assertEquals(1, namespaces.size());
        assertEquals("marketing", namespaces.get(0).id());
        assertEquals("Marketing", namespaces.get(0).title());
        assertEquals(1, namespaces.get(0).tagCount());
    }

    @Test
    void testGetTag() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jcr:title", "Summer");
        when(mockClient.get(eq("/content/cq:tags/marketing/campaigns/summer.json")))
            .thenReturn(response);

        Tag tag = tagsApi.getTag("marketing:campaigns/summer");

        assertEquals("marketing:campaigns/summer", tag.tagId());
        assertEquals("Summer", tag.title());
    }

    @Test
    void testCreateTagPostsParentsThenGets() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jcr:title", "Summer");
        when(mockClient.get(contains("/content/cq:tags/marketing/campaigns/summer.json")))
            .thenReturn(response);

        Tag tag = tagsApi.createTag("marketing:campaigns/summer", "Summer", "desc");

        verify(mockClient, atLeast(2)).post(anyString(), any());
        assertEquals("Summer", tag.title());
    }

    @Test
    void testCreateTagIgnoresConflictErrors() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jcr:title", "Summer");
        when(mockClient.post(anyString(), any()))
            .thenThrow(new IOException("HTTP 409 Conflict - already exists"));
        when(mockClient.get(anyString())).thenReturn(response);

        assertDoesNotThrow(() -> tagsApi.createTag("marketing:summer", "Summer", null));
    }

    @Test
    void testCreateNamespace() throws IOException {
        TagNamespace ns = tagsApi.createNamespace("geo", "Geography", null);

        assertEquals("geo", ns.id());
        assertEquals("Geography", ns.title());
        verify(mockClient).post(eq("/content/cq:tags/geo"), any());
    }

    @Test
    void testUpdateTag() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jcr:title", "New Title");
        when(mockClient.get(anyString())).thenReturn(response);

        Tag tag = tagsApi.updateTag("geo:us", "New Title", null);

        assertEquals("New Title", tag.title());
        verify(mockClient).post(eq("/content/cq:tags/geo/us"), any());
    }

    @Test
    void testDeleteTagWithForce() throws IOException {
        when(mockClient.delete(eq("/content/cq:tags/geo/us"))).thenReturn(true);

        assertTrue(tagsApi.deleteTag("geo:us", true));
        verify(mockClient, never()).get(contains("querybuilder"));
    }

    @Test
    void testDeleteTagChecksUsageWhenNotForced() throws IOException {
        ObjectNode usage = mapper.createObjectNode();
        usage.put("total", 0);
        when(mockClient.get(contains("querybuilder"))).thenReturn(usage);
        when(mockClient.delete(anyString())).thenReturn(true);

        assertTrue(tagsApi.deleteTag("geo:us", false));
    }

    @Test
    void testDeleteTagInUseThrows() throws IOException {
        ObjectNode usage = mapper.createObjectNode();
        usage.put("total", 3);
        when(mockClient.get(contains("querybuilder"))).thenReturn(usage);

        assertThrows(IOException.class, () -> tagsApi.deleteTag("geo:us", false));
    }

    @Test
    void testMoveTag() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jcr:title", "Moved");
        when(mockClient.move(anyString(), anyString())).thenReturn(mapper.createObjectNode());
        when(mockClient.get(contains("/content/cq:tags/geo/north.json"))).thenReturn(response);

        Tag tag = tagsApi.moveTag("geo:south", "geo:north");

        assertEquals("geo:north", tag.tagId());
        verify(mockClient).move(
            eq("/content/cq:tags/geo/south"), eq("/content/cq:tags/geo/north"));
    }

    @Test
    void testMergeTagsRetagsAndDeletes() throws IOException {
        ObjectNode search = mapper.createObjectNode();
        ArrayNode hits = mapper.createArrayNode();
        ObjectNode hit = mapper.createObjectNode();
        hit.put("jcr:path", "/content/page");
        hits.add(hit);
        search.set("hits", hits);

        ObjectNode content = mapper.createObjectNode();
        ArrayNode tags = mapper.createArrayNode();
        tags.add("old:tag");
        tags.add("keep:tag");
        content.set("cq:tags", tags);

        when(mockClient.get(anyString())).thenReturn(search, content);
        when(mockClient.delete(anyString())).thenReturn(true);

        int retagged = tagsApi.mergeTags("old:tag", "new:tag");

        assertEquals(1, retagged);
        verify(mockClient).post(eq("/content/page"), any());
    }

    @Test
    void testGetTagUsageCount() throws IOException {
        ObjectNode usage = mapper.createObjectNode();
        usage.put("total", 7);
        when(mockClient.get(contains("property=cq:tags"))).thenReturn(usage);

        assertEquals(7, tagsApi.getTagUsageCount("geo:us"));
    }

    @Test
    void testGetTaggedContent() throws IOException {
        ObjectNode search = mapper.createObjectNode();
        ArrayNode hits = mapper.createArrayNode();
        ObjectNode hit = mapper.createObjectNode();
        hit.put("jcr:path", "/content/page-a");
        hits.add(hit);
        search.set("hits", hits);
        when(mockClient.get(contains("querybuilder"))).thenReturn(search);

        List<String> paths = tagsApi.getTaggedContent("geo:us", 100);

        assertEquals(1, paths.size());
        assertEquals("/content/page-a", paths.get(0));
    }

    @Test
    void testApplyTagsReplace() throws IOException {
        when(mockClient.post(anyString(), any())).thenReturn(mapper.createObjectNode());

        assertTrue(tagsApi.applyTags("/content/page", List.of("geo:us", "geo:ca"), true));
        verify(mockClient).post(eq("/content/page"), any());
    }

    @Test
    void testApplyTagsAppendMergesExisting() throws IOException {
        ObjectNode content = mapper.createObjectNode();
        ArrayNode existing = mapper.createArrayNode();
        existing.add("geo:us");
        content.set("cq:tags", existing);
        when(mockClient.get(anyString())).thenReturn(content);
        when(mockClient.post(anyString(), any())).thenReturn(mapper.createObjectNode());

        assertTrue(tagsApi.applyTags("/content/page", List.of("geo:ca"), false));
        verify(mockClient).post(eq("/content/page"), any());
    }

    @Test
    void testRemoveTags() throws IOException {
        ObjectNode content = mapper.createObjectNode();
        ArrayNode tags = mapper.createArrayNode();
        tags.add("geo:us");
        tags.add("geo:ca");
        content.set("cq:tags", tags);
        when(mockClient.get(anyString())).thenReturn(content);
        when(mockClient.post(anyString(), any())).thenReturn(mapper.createObjectNode());

        assertTrue(tagsApi.removeTags("/content/page", List.of("geo:us")));
        verify(mockClient).post(eq("/content/page"), any());
    }

    @Test
    void testSetLocalizedTitle() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jcr:title", "Sommer");
        when(mockClient.get(anyString())).thenReturn(response);

        Tag tag = tagsApi.setLocalizedTitle("geo:us", "de", "Sommer");

        assertEquals("Sommer", tag.title());
        verify(mockClient).post(eq("/content/cq:tags/geo/us"), any());
    }

    @Test
    void testGetLocalizedTitles() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("jcr:title", "United States");
        response.put("jcr:title.de", "Vereinigte Staaten");
        response.put("jcr:title.fr", "États-Unis");
        when(mockClient.get(anyString())).thenReturn(response);

        Map<String, String> titles = tagsApi.getLocalizedTitles("geo:us");

        assertEquals("Vereinigte Staaten", titles.get("de"));
        assertEquals("États-Unis", titles.get("fr"));
        assertEquals("United States", titles.get("default"));
    }
}
