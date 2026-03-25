package com.aemtools.aem.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

public class MockDataHelper {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static JsonNode getContentFragments() {
        ArrayNode array = mapper.createArrayNode();
        
        ObjectNode cf1 = mapper.createObjectNode();
        cf1.put("name", "my-fragment-1");
        cf1.put("title", "My Content Fragment 1");
        cf1.put("model", "/conf/my-project/settings/dam/cfm/models/my-model");
        cf1.put("path", "/content/dam/my-project/fragments/my-fragment-1");
        cf1.put("description", "Sample content fragment");
        array.add(cf1);
        
        ObjectNode cf2 = mapper.createObjectNode();
        cf2.put("name", "my-fragment-2");
        cf2.put("title", "My Content Fragment 2");
        cf2.put("model", "/conf/my-project/settings/dam/cfm/models/my-model");
        cf2.put("path", "/content/dam/my-project/fragments/my-fragment-2");
        cf2.put("description", "Another sample");
        array.add(cf2);
        
        return array;
    }

    public static JsonNode getAssets() {
        ArrayNode array = mapper.createArrayNode();
        
        ObjectNode asset1 = mapper.createObjectNode();
        asset1.put("name", "hero-image.jpg");
        asset1.put("title", "Hero Image");
        asset1.put("path", "/content/dam/my-project/hero-image.jpg");
        asset1.put("mimeType", "image/jpeg");
        asset1.put("size", 1024000);
        array.add(asset1);
        
        ObjectNode asset2 = mapper.createObjectNode();
        asset2.put("name", "document.pdf");
        asset2.put("title", "Product Document");
        asset2.put("path", "/content/dam/my-project/document.pdf");
        asset2.put("mimeType", "application/pdf");
        asset2.put("size", 2048000);
        array.add(asset2);
        
        return array;
    }

    public static JsonNode getWorkflows() {
        ArrayNode array = mapper.createArrayNode();
        
        ObjectNode wf1 = mapper.createObjectNode();
        wf1.put("id", "workflow-instance-001");
        wf1.put("modelTitle", "DAM Update Asset");
        wf1.put("status", "RUNNING");
        wf1.put("startTime", "2024-01-15T10:30:00Z");
        array.add(wf1);
        
        ObjectNode wf2 = mapper.createObjectNode();
        wf2.put("id", "workflow-instance-002");
        wf2.put("modelTitle", "DAM Workflow");
        wf2.put("status", "COMPLETED");
        wf2.put("startTime", "2024-01-14T09:00:00Z");
        wf2.put("completionTime", "2024-01-14T09:15:00Z");
        array.add(wf2);
        
        return array;
    }

    public static JsonNode getPackages() {
        ArrayNode array = mapper.createArrayNode();
        
        ObjectNode pkg1 = mapper.createObjectNode();
        pkg1.put("path", "/etc/packages/my_packages/my-package-1.zip");
        pkg1.put("group", "my_packages");
        pkg1.put("name", "my-package-1");
        pkg1.put("version", "1.0.0");
        pkg1.put("description", "My first package");
        pkg1.put("size", 512000);
        pkg1.put("installed", true);
        pkg1.put("lastBuilt", "2024-01-15T08:00:00Z");
        array.add(pkg1);
        
        ObjectNode pkg2 = mapper.createObjectNode();
        pkg2.put("path", "/etc/packages/my_packages/my-package-2.zip");
        pkg2.put("group", "my_packages");
        pkg2.put("name", "my-package-2");
        pkg2.put("version", "2.0.0");
        pkg2.put("description", "Second package");
        pkg2.put("size", 1024000);
        pkg2.put("installed", false);
        array.add(pkg2);
        
        return array;
    }

    public static JsonNode getTags() {
        ArrayNode array = mapper.createArrayNode();
        
        ObjectNode tag1 = mapper.createObjectNode();
        tag1.put("tagID", "marketing:campaign");
        tag1.put("title", "Campaign");
        array.add(tag1);
        
        ObjectNode tag2 = mapper.createObjectNode();
        tag2.put("tagID", "products:electronics");
        tag2.put("title", "Electronics");
        array.add(tag2);
        
        ObjectNode tag3 = mapper.createObjectNode();
        tag3.put("tagID", "region:us");
        tag3.put("title", "United States");
        array.add(tag3);
        
        return array;
    }

    public static JsonNode getUsers() {
        ArrayNode array = mapper.createArrayNode();
        
        ObjectNode user1 = mapper.createObjectNode();
        user1.put("name", "admin");
        user1.put("rep:authorizableId", "admin");
        user1.put("path", "/home/users/a/admin");
        array.add(user1);
        
        ObjectNode user2 = mapper.createObjectNode();
        user2.put("name", "jdoe");
        user2.put("rep:authorizableId", "jdoe");
        user2.put("path", "/home/users/j/jdoe");
        array.add(user2);
        
        ObjectNode user3 = mapper.createObjectNode();
        user3.put("name", "content-author");
        user3.put("rep:authorizableId", "content-author");
        user3.put("path", "/home/users/c/content-author");
        array.add(user3);
        
        return array;
    }

    public static JsonNode getModels() {
        ArrayNode array = mapper.createArrayNode();
        
        ObjectNode model1 = mapper.createObjectNode();
        model1.put("name", "my-model");
        model1.put("title", "My Content Fragment Model");
        model1.put("path", "/conf/my-project/settings/dam/cfm/models/my-model");
        array.add(model1);
        
        ObjectNode model2 = mapper.createObjectNode();
        model2.put("name", "product-model");
        model2.put("title", "Product Model");
        model2.put("path", "/conf/my-project/settings/dam/cfm/models/product-model");
        array.add(model2);
        
        return array;
    }

    public static JsonNode getReplicationStatus() {
        ObjectNode status = mapper.createObjectNode();
        status.put("path", "/content/we-retail/us/en");
        status.put("published", true);
        status.put("lastPublished", "2024-01-15T12:00:00Z");
        status.put("count", 5);
        return status;
    }

    public static JsonNode getQueueStatus() {
        ObjectNode status = mapper.createObjectNode();
        status.put("queued", 3);
        status.put("processing", 1);
        status.put("failed", 0);
        return status;
    }

    public static JsonNode successResponse(String message) {
        ObjectNode response = mapper.createObjectNode();
        response.put("success", true);
        response.put("message", message);
        return response;
    }
}
