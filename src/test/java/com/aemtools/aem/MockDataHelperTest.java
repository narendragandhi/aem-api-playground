package com.aemtools.aem;

import com.aemtools.aem.util.MockDataHelper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MockDataHelperTest {

    @Test
    void testGetContentFragments() {
        JsonNode data = MockDataHelper.getContentFragments();
        assertNotNull(data);
        assertTrue(data.isArray());
        assertEquals(2, data.size());
        
        JsonNode cf1 = data.get(0);
        assertEquals("my-fragment-1", cf1.get("name").asText());
        assertEquals("My Content Fragment 1", cf1.get("title").asText());
        assertTrue(cf1.has("model"));
        assertTrue(cf1.has("path"));
    }

    @Test
    void testGetAssets() {
        JsonNode data = MockDataHelper.getAssets();
        assertNotNull(data);
        assertTrue(data.isArray());
        assertEquals(2, data.size());
        
        JsonNode asset1 = data.get(0);
        assertEquals("hero-image.jpg", asset1.get("name").asText());
        assertEquals("image/jpeg", asset1.get("mimeType").asText());
    }

    @Test
    void testGetWorkflows() {
        JsonNode data = MockDataHelper.getWorkflows();
        assertNotNull(data);
        assertTrue(data.isArray());
        assertEquals(2, data.size());
        
        JsonNode wf1 = data.get(0);
        assertTrue(wf1.has("id"));
        assertTrue(wf1.has("modelTitle"));
        assertTrue(wf1.has("status"));
    }

    @Test
    void testGetPackages() {
        JsonNode data = MockDataHelper.getPackages();
        assertNotNull(data);
        assertTrue(data.isArray());
        assertEquals(2, data.size());
        
        JsonNode pkg1 = data.get(0);
        assertEquals("my_packages", pkg1.get("group").asText());
        assertEquals("my-package-1", pkg1.get("name").asText());
        assertEquals("1.0.0", pkg1.get("version").asText());
    }

    @Test
    void testGetTags() {
        JsonNode data = MockDataHelper.getTags();
        assertNotNull(data);
        assertTrue(data.isArray());
        assertEquals(3, data.size());
        
        JsonNode tag1 = data.get(0);
        assertEquals("marketing:campaign", tag1.get("tagID").asText());
    }

    @Test
    void testGetUsers() {
        JsonNode data = MockDataHelper.getUsers();
        assertNotNull(data);
        assertTrue(data.isArray());
        assertEquals(3, data.size());
        
        JsonNode user1 = data.get(0);
        assertTrue(user1.has("name"));
        assertTrue(user1.has("rep:authorizableId"));
    }

    @Test
    void testGetModels() {
        JsonNode data = MockDataHelper.getModels();
        assertNotNull(data);
        assertTrue(data.isArray());
        assertEquals(2, data.size());
    }

    @Test
    void testGetReplicationStatus() {
        JsonNode data = MockDataHelper.getReplicationStatus();
        assertNotNull(data);
        assertEquals("/content/we-retail/us/en", data.get("path").asText());
        assertTrue(data.get("published").asBoolean());
    }

    @Test
    void testGetQueueStatus() {
        JsonNode data = MockDataHelper.getQueueStatus();
        assertNotNull(data);
        assertEquals(3, data.get("queued").asInt());
        assertEquals(1, data.get("processing").asInt());
        assertEquals(0, data.get("failed").asInt());
    }

    @Test
    void testSuccessResponse() {
        JsonNode data = MockDataHelper.successResponse("Test message");
        assertNotNull(data);
        assertTrue(data.get("success").asBoolean());
        assertEquals("Test message", data.get("message").asText());
    }
}
