package com.aemtools.aem;

import com.aemtools.aem.api.WorkflowApi;
import com.aemtools.aem.api.WorkflowApi.*;
import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

/**
 * Integration tests for WorkflowApi using mocked HTTP client.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WorkflowApi Mock Tests")
class WorkflowApiMockTest {

    @Mock
    private AemApiClient mockClient;

    private WorkflowApi workflowApi;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        when(mockClient.getObjectMapper()).thenReturn(mapper);
        workflowApi = new WorkflowApi(mockClient);
    }

    @Nested
    @DisplayName("listModels Tests")
    class ListModelsTests {

        @Test
        @DisplayName("Should return empty list when no models exist")
        void testListModelsEmpty() throws IOException {
            ObjectNode emptyResponse = mapper.createObjectNode();
            when(mockClient.get(contains("workflow/models"))).thenReturn(emptyResponse);

            List<WorkflowModel> models = workflowApi.listModels();

            assertTrue(models.isEmpty());
        }

        @Test
        @DisplayName("Should parse workflow models correctly")
        void testListModelsWithData() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            ObjectNode model1 = mapper.createObjectNode();
            model1.put("jcr:title", "Request for Activation");
            model1.put("modelId", "request_for_activation");
            model1.put("version", "1.0");
            response.set("request_for_activation", model1);

            ObjectNode model2 = mapper.createObjectNode();
            model2.put("jcr:title", "DAM Update Asset");
            model2.put("modelId", "dam_update_asset");
            response.set("dam_update_asset", model2);

            // Add jcr: properties that should be skipped
            response.put("jcr:primaryType", "nt:folder");

            when(mockClient.get(contains("workflow/models"))).thenReturn(response);

            List<WorkflowModel> models = workflowApi.listModels();

            assertEquals(2, models.size());
            assertTrue(models.stream().anyMatch(m -> "Request for Activation".equals(m.getTitle())));
            assertTrue(models.stream().anyMatch(m -> "DAM Update Asset".equals(m.getTitle())));
        }
    }

    @Nested
    @DisplayName("listInstances Tests")
    class ListInstancesTests {

        @Test
        @DisplayName("Should list running workflow instances")
        void testListRunningInstances() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            ObjectNode instance1 = mapper.createObjectNode();
            instance1.put("id", "wf-001");
            instance1.put("modelTitle", "Request for Activation");
            instance1.put("status", "RUNNING");
            instance1.put("payload", "/content/dam/test.jpg");
            instance1.put("initiator", "admin");
            response.set("wf-001", instance1);

            when(mockClient.get(contains("workflow/instances"))).thenReturn(response);

            List<WorkflowInstance> instances = workflowApi.listRunningInstances(10);

            assertEquals(1, instances.size());
            assertEquals("wf-001", instances.get(0).getId());
            assertEquals("RUNNING", instances.get(0).getStatus());
        }

        @Test
        @DisplayName("Should respect limit parameter")
        void testListInstancesWithLimit() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            for (int i = 0; i < 10; i++) {
                ObjectNode instance = mapper.createObjectNode();
                instance.put("id", "wf-" + i);
                instance.put("status", "COMPLETED");
                response.set("wf-" + i, instance);
            }

            when(mockClient.get(contains("workflow/instances"))).thenReturn(response);

            List<WorkflowInstance> instances = workflowApi.listInstances(WorkflowStatus.COMPLETED, 5);

            assertEquals(5, instances.size());
        }

        @Test
        @DisplayName("Should handle array response format")
        void testListInstancesArrayFormat() throws IOException {
            ArrayNode response = mapper.createArrayNode();
            ObjectNode instance = mapper.createObjectNode();
            instance.put("id", "wf-array-001");
            instance.put("status", "RUNNING");
            response.add(instance);

            when(mockClient.get(contains("workflow/instances"))).thenReturn(response);

            List<WorkflowInstance> instances = workflowApi.listInstances(null, 10);

            assertEquals(1, instances.size());
            assertEquals("wf-array-001", instances.get(0).getId());
        }
    }

    @Nested
    @DisplayName("startWorkflow Tests")
    class StartWorkflowTests {

        @Test
        @DisplayName("Should start workflow with model and payload")
        void testStartWorkflow() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            response.put("id", "wf-new-001");
            response.put("status", "RUNNING");
            response.put("modelTitle", "Request for Activation");
            response.put("payload", "/content/dam/test.jpg");

            when(mockClient.post(contains("workflow/instances"), any())).thenReturn(response);

            WorkflowInstance instance = workflowApi.startWorkflow(
                "/var/workflow/models/request_for_activation",
                "/content/dam/test.jpg"
            );

            assertNotNull(instance);
            assertEquals("wf-new-001", instance.getId());
            assertEquals("RUNNING", instance.getStatus());

            verify(mockClient).post(contains("workflow/instances"), any());
        }

        @Test
        @DisplayName("Should include metadata when provided")
        void testStartWorkflowWithMetadata() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            response.put("id", "wf-meta-001");
            response.put("status", "RUNNING");

            when(mockClient.post(anyString(), any())).thenReturn(response);

            Map<String, String> metadata = Map.of("comment", "Test workflow", "priority", "high");
            WorkflowInstance instance = workflowApi.startWorkflow(
                "/var/workflow/models/approval",
                "/content/test",
                "JCR_PATH",
                metadata
            );

            assertNotNull(instance);
            assertEquals("wf-meta-001", instance.getId());
        }

        @Test
        @DisplayName("Should fallback to classic endpoint on error")
        void testStartWorkflowFallback() throws IOException {
            // First call fails, second succeeds
            when(mockClient.post(contains("api/workflow"), any()))
                .thenThrow(new IOException("API not found"));

            ObjectNode fallbackResponse = mapper.createObjectNode();
            fallbackResponse.put("id", "wf-fallback-001");

            when(mockClient.post(contains("etc/workflow"), any()))
                .thenReturn(fallbackResponse);

            WorkflowInstance instance = workflowApi.startWorkflow(
                "/var/workflow/models/test",
                "/content/test"
            );

            assertNotNull(instance);
            assertEquals("wf-fallback-001", instance.getId());
        }
    }

    @Nested
    @DisplayName("terminateWorkflow Tests")
    class TerminateWorkflowTests {

        @Test
        @DisplayName("Should terminate workflow successfully")
        void testTerminateWorkflow() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            response.put("success", true);

            when(mockClient.post(contains("wf-001"), any())).thenReturn(response);

            boolean result = workflowApi.terminateWorkflow("wf-001");

            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle full path for instance ID")
        void testTerminateWorkflowFullPath() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            when(mockClient.post(startsWith("/etc/workflow/instances"), any())).thenReturn(response);

            boolean result = workflowApi.terminateWorkflow("/etc/workflow/instances/server0/wf-001");

            assertTrue(result);
            verify(mockClient).post(eq("/etc/workflow/instances/server0/wf-001"), any());
        }
    }

    @Nested
    @DisplayName("suspendWorkflow Tests")
    class SuspendWorkflowTests {

        @Test
        @DisplayName("Should suspend workflow successfully")
        void testSuspendWorkflow() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            when(mockClient.post(anyString(), any())).thenReturn(response);

            boolean result = workflowApi.suspendWorkflow("wf-001");

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("resumeWorkflow Tests")
    class ResumeWorkflowTests {

        @Test
        @DisplayName("Should resume workflow successfully")
        void testResumeWorkflow() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            when(mockClient.post(anyString(), any())).thenReturn(response);

            boolean result = workflowApi.resumeWorkflow("wf-001");

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("listWorkItems Tests")
    class ListWorkItemsTests {

        @Test
        @DisplayName("Should list work items from inbox")
        void testListWorkItems() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            ArrayNode workflows = mapper.createArrayNode();

            ObjectNode item1 = mapper.createObjectNode();
            item1.put("id", "item-001");
            item1.put("title", "Review Content");
            item1.put("assignee", "admin");
            item1.put("stepTitle", "Approval");
            workflows.add(item1);

            response.set("workflows", workflows);

            when(mockClient.get(contains("workflow/inbox"))).thenReturn(response);

            List<WorkItem> items = workflowApi.listWorkItems(10);

            assertEquals(1, items.size());
            assertEquals("item-001", items.get(0).getId());
            assertEquals("Review Content", items.get(0).getTitle());
        }

        @Test
        @DisplayName("Should handle array response format")
        void testListWorkItemsArrayFormat() throws IOException {
            ArrayNode response = mapper.createArrayNode();
            ObjectNode item = mapper.createObjectNode();
            item.put("itemId", "item-002");
            item.put("name", "Approve Publication");
            response.add(item);

            when(mockClient.get(contains("workflow/inbox"))).thenReturn(response);

            List<WorkItem> items = workflowApi.listWorkItems(10);

            assertEquals(1, items.size());
            assertEquals("item-002", items.get(0).getId());
        }
    }

    @Nested
    @DisplayName("completeWorkItem Tests")
    class CompleteWorkItemTests {

        @Test
        @DisplayName("Should complete work item successfully")
        void testCompleteWorkItem() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            when(mockClient.post(contains("workflow/inbox"), any())).thenReturn(response);

            boolean result = workflowApi.completeWorkItem("item-001", null, null);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should complete work item with route and comment")
        void testCompleteWorkItemWithRouteAndComment() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            when(mockClient.post(contains("workflow/inbox"), any())).thenReturn(response);

            boolean result = workflowApi.completeWorkItem("item-001", "approve", "Looks good");

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("getHistory Tests")
    class GetHistoryTests {

        @Test
        @DisplayName("Should get workflow history for content path")
        void testGetHistory() throws IOException {
            ArrayNode response = mapper.createArrayNode();
            ObjectNode historyItem = mapper.createObjectNode();
            historyItem.put("workflowId", "wf-hist-001");
            historyItem.put("modelTitle", "Request for Activation");
            historyItem.put("status", "COMPLETED");
            historyItem.put("initiator", "author");
            response.add(historyItem);

            when(mockClient.get(contains("workflow/history"))).thenReturn(response);

            List<WorkflowHistoryItem> history = workflowApi.getHistory("/content/dam/test.jpg");

            assertEquals(1, history.size());
            assertEquals("wf-hist-001", history.get(0).getWorkflowId());
            assertEquals("COMPLETED", history.get(0).getStatus());
        }
    }

    @Nested
    @DisplayName("Bulk Operations Tests")
    class BulkOperationsTests {

        @Test
        @DisplayName("Should start bulk workflows")
        void testStartBulkWorkflows() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            response.put("id", "wf-bulk");
            response.put("status", "RUNNING");

            when(mockClient.post(anyString(), any())).thenReturn(response);

            List<String> payloads = List.of("/content/dam/a.jpg", "/content/dam/b.jpg", "/content/dam/c.jpg");
            Map<String, String> results = workflowApi.startBulkWorkflows(
                "/var/workflow/models/request_for_activation",
                payloads
            );

            assertEquals(3, results.size());
            assertTrue(results.values().stream().allMatch(v -> v.equals("wf-bulk")));
        }

        @Test
        @DisplayName("Should handle errors in bulk workflow start")
        void testStartBulkWorkflowsWithErrors() throws IOException {
            // startWorkflow tries api/workflow first, then etc/workflow as fallback
            // For /content/a - api/workflow succeeds
            // For /content/b - both api/workflow and etc/workflow fail
            // For /content/c - api/workflow succeeds
            when(mockClient.post(anyString(), any()))
                .thenReturn(mapper.createObjectNode().put("id", "wf-1"))  // /content/a succeeds
                .thenThrow(new IOException("API failed"))                  // /content/b api fails
                .thenThrow(new IOException("Fallback failed"))             // /content/b fallback fails
                .thenReturn(mapper.createObjectNode().put("id", "wf-3")); // /content/c succeeds

            List<String> payloads = List.of("/content/a", "/content/b", "/content/c");
            Map<String, String> results = workflowApi.startBulkWorkflows("/model", payloads);

            assertEquals(3, results.size());
            // First call succeeds
            assertEquals("wf-1", results.get("/content/a"));
            // Second call fails - should contain error message
            String errorResult = results.get("/content/b");
            assertTrue(errorResult != null && errorResult.startsWith("ERROR:"),
                "Expected error for /content/b but got: " + errorResult);
            // Third call succeeds
            assertEquals("wf-3", results.get("/content/c"));
        }

        @Test
        @DisplayName("Should terminate bulk workflows")
        void testTerminateBulkWorkflows() throws IOException {
            when(mockClient.post(anyString(), any())).thenReturn(mapper.createObjectNode());

            List<String> instanceIds = List.of("wf-1", "wf-2", "wf-3");
            Map<String, Boolean> results = workflowApi.terminateBulkWorkflows(instanceIds);

            assertEquals(3, results.size());
            assertTrue(results.values().stream().allMatch(v -> v));
        }
    }

    @Nested
    @DisplayName("purgeCompletedWorkflows Tests")
    class PurgeWorkflowsTests {

        @Test
        @DisplayName("Should purge completed workflows")
        void testPurgeCompletedWorkflows() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            response.put("purged", 25);

            when(mockClient.post(contains("purge"), any())).thenReturn(response);

            int purged = workflowApi.purgeCompletedWorkflows(30);

            assertEquals(25, purged);
        }

        @Test
        @DisplayName("Should return 0 when purged field is missing")
        void testPurgeCompletedWorkflowsNoPurgedField() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            when(mockClient.post(contains("purge"), any())).thenReturn(response);

            int purged = workflowApi.purgeCompletedWorkflows(7);

            assertEquals(0, purged);
        }
    }

    @Nested
    @DisplayName("getInstance Tests")
    class GetInstanceTests {

        @Test
        @DisplayName("Should get workflow instance details")
        void testGetInstance() throws IOException {
            ObjectNode response = mapper.createObjectNode();
            response.put("id", "wf-detail-001");
            response.put("modelTitle", "DAM Update Asset");
            response.put("status", "RUNNING");
            response.put("payload", "/content/dam/photo.jpg");
            response.put("initiator", "system");
            response.put("startTime", "2024-01-15T10:00:00Z");
            response.put("currentStep", "Processing");

            when(mockClient.get(contains("wf-detail-001"))).thenReturn(response);

            WorkflowInstance instance = workflowApi.getInstance("wf-detail-001");

            assertNotNull(instance);
            assertEquals("wf-detail-001", instance.getId());
            assertEquals("DAM Update Asset", instance.getModelTitle());
            assertEquals("RUNNING", instance.getStatus());
            assertEquals("Processing", instance.getCurrentStep());
        }
    }
}
