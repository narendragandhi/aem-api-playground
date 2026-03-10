package com.aemtools.aem;

import com.aemtools.aem.api.WorkflowApi;
import com.aemtools.aem.api.WorkflowApi.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WorkflowApi data classes and parsing logic.
 * Note: These tests focus on the data classes and parsing behavior.
 * Integration tests with a real AEM instance would require mocking the HTTP client.
 */
@DisplayName("WorkflowApi Tests")
class WorkflowApiTest {

    @Nested
    @DisplayName("WorkflowModel Tests")
    class WorkflowModelTests {

        @Test
        @DisplayName("WorkflowModel should store and retrieve all properties")
        void testWorkflowModelProperties() {
            WorkflowModel model = new WorkflowModel();
            model.setPath("/var/workflow/models/request_for_activation");
            model.setId("request_for_activation");
            model.setTitle("Request for Activation");
            model.setDescription("Workflow to request content activation");
            model.setVersion("1.0");
            model.setTransient(false);

            assertEquals("/var/workflow/models/request_for_activation", model.getPath());
            assertEquals("request_for_activation", model.getId());
            assertEquals("Request for Activation", model.getTitle());
            assertEquals("Workflow to request content activation", model.getDescription());
            assertEquals("1.0", model.getVersion());
            assertFalse(model.isTransient());
        }

        @Test
        @DisplayName("WorkflowModel toString should include title, version, and path")
        void testWorkflowModelToString() {
            WorkflowModel model = new WorkflowModel();
            model.setTitle("DAM Update Asset");
            model.setVersion("2.0");
            model.setPath("/var/workflow/models/dam/update_asset");

            String result = model.toString();
            assertTrue(result.contains("DAM Update Asset"));
            assertTrue(result.contains("2.0"));
            assertTrue(result.contains("/var/workflow/models/dam/update_asset"));
        }

        @Test
        @DisplayName("WorkflowModel should handle transient workflows")
        void testTransientWorkflow() {
            WorkflowModel model = new WorkflowModel();
            model.setTransient(true);

            assertTrue(model.isTransient());
        }
    }

    @Nested
    @DisplayName("WorkflowInstance Tests")
    class WorkflowInstanceTests {

        @Test
        @DisplayName("WorkflowInstance should store and retrieve all properties")
        void testWorkflowInstanceProperties() {
            WorkflowInstance instance = new WorkflowInstance();
            instance.setPath("/etc/workflow/instances/server0/2024-01-15/wf-12345");
            instance.setId("wf-12345");
            instance.setModelId("/var/workflow/models/request_for_activation");
            instance.setModelTitle("Request for Activation");
            instance.setPayload("/content/dam/test.jpg");
            instance.setStatus("RUNNING");
            instance.setInitiator("admin");
            instance.setStartTime("2024-01-15T10:30:00Z");
            instance.setEndTime(null);
            instance.setCurrentStep("Approval");

            assertEquals("/etc/workflow/instances/server0/2024-01-15/wf-12345", instance.getPath());
            assertEquals("wf-12345", instance.getId());
            assertEquals("/var/workflow/models/request_for_activation", instance.getModelId());
            assertEquals("Request for Activation", instance.getModelTitle());
            assertEquals("/content/dam/test.jpg", instance.getPayload());
            assertEquals("RUNNING", instance.getStatus());
            assertEquals("admin", instance.getInitiator());
            assertEquals("2024-01-15T10:30:00Z", instance.getStartTime());
            assertNull(instance.getEndTime());
            assertEquals("Approval", instance.getCurrentStep());
        }

        @Test
        @DisplayName("WorkflowInstance toString should include id, model title, status, and payload")
        void testWorkflowInstanceToString() {
            WorkflowInstance instance = new WorkflowInstance();
            instance.setId("wf-67890");
            instance.setModelTitle("DAM Update Asset");
            instance.setStatus("COMPLETED");
            instance.setPayload("/content/dam/image.png");

            String result = instance.toString();
            assertTrue(result.contains("wf-67890"));
            assertTrue(result.contains("DAM Update Asset"));
            assertTrue(result.contains("COMPLETED"));
            assertTrue(result.contains("/content/dam/image.png"));
        }

        @Test
        @DisplayName("WorkflowInstance should handle completed workflows")
        void testCompletedWorkflowInstance() {
            WorkflowInstance instance = new WorkflowInstance();
            instance.setStatus("COMPLETED");
            instance.setStartTime("2024-01-15T10:00:00Z");
            instance.setEndTime("2024-01-15T10:15:00Z");

            assertEquals("COMPLETED", instance.getStatus());
            assertNotNull(instance.getEndTime());
        }
    }

    @Nested
    @DisplayName("WorkItem Tests")
    class WorkItemTests {

        @Test
        @DisplayName("WorkItem should store and retrieve all properties")
        void testWorkItemProperties() {
            WorkItem item = new WorkItem();
            item.setId("item-001");
            item.setWorkflowId("wf-12345");
            item.setTitle("Review Content");
            item.setPayload("/content/dam/document.pdf");
            item.setAssignee("reviewer");
            item.setStepTitle("Content Review");
            item.setStartTime("2024-01-15T09:00:00Z");
            item.setDueTime("2024-01-16T09:00:00Z");
            item.setComment("Please review urgently");

            assertEquals("item-001", item.getId());
            assertEquals("wf-12345", item.getWorkflowId());
            assertEquals("Review Content", item.getTitle());
            assertEquals("/content/dam/document.pdf", item.getPayload());
            assertEquals("reviewer", item.getAssignee());
            assertEquals("Content Review", item.getStepTitle());
            assertEquals("2024-01-15T09:00:00Z", item.getStartTime());
            assertEquals("2024-01-16T09:00:00Z", item.getDueTime());
            assertEquals("Please review urgently", item.getComment());
        }

        @Test
        @DisplayName("WorkItem toString should include id, title, step, and assignee")
        void testWorkItemToString() {
            WorkItem item = new WorkItem();
            item.setId("item-002");
            item.setTitle("Approve Publication");
            item.setStepTitle("Final Approval");
            item.setAssignee("editor");

            String result = item.toString();
            assertTrue(result.contains("item-002"));
            assertTrue(result.contains("Approve Publication"));
            assertTrue(result.contains("Final Approval"));
            assertTrue(result.contains("editor"));
        }
    }

    @Nested
    @DisplayName("WorkflowHistoryItem Tests")
    class WorkflowHistoryItemTests {

        @Test
        @DisplayName("WorkflowHistoryItem should store and retrieve all properties")
        void testWorkflowHistoryItemProperties() {
            WorkflowHistoryItem item = new WorkflowHistoryItem();
            item.setWorkflowId("wf-history-001");
            item.setModelTitle("Request for Activation");
            item.setStatus("COMPLETED");
            item.setInitiator("content-author");
            item.setStartTime("2024-01-10T14:00:00Z");
            item.setEndTime("2024-01-10T14:30:00Z");

            assertEquals("wf-history-001", item.getWorkflowId());
            assertEquals("Request for Activation", item.getModelTitle());
            assertEquals("COMPLETED", item.getStatus());
            assertEquals("content-author", item.getInitiator());
            assertEquals("2024-01-10T14:00:00Z", item.getStartTime());
            assertEquals("2024-01-10T14:30:00Z", item.getEndTime());
        }

        @Test
        @DisplayName("WorkflowHistoryItem toString should include workflow id, model, status, and initiator")
        void testWorkflowHistoryItemToString() {
            WorkflowHistoryItem item = new WorkflowHistoryItem();
            item.setWorkflowId("wf-past-123");
            item.setModelTitle("DAM Update Asset");
            item.setStatus("ABORTED");
            item.setInitiator("system");

            String result = item.toString();
            assertTrue(result.contains("wf-past-123"));
            assertTrue(result.contains("DAM Update Asset"));
            assertTrue(result.contains("ABORTED"));
            assertTrue(result.contains("system"));
        }
    }

    @Nested
    @DisplayName("WorkflowStats Tests")
    class WorkflowStatsTests {

        @Test
        @DisplayName("WorkflowStats should store and retrieve all properties")
        void testWorkflowStatsProperties() {
            WorkflowStats stats = new WorkflowStats();
            stats.setRunning(5);
            stats.setCompleted(150);
            stats.setSuspended(2);
            stats.setAborted(3);
            stats.setStale(1);
            stats.setPendingWorkItems(8);
            stats.setAvailableModels(12);

            assertEquals(5, stats.getRunning());
            assertEquals(150, stats.getCompleted());
            assertEquals(2, stats.getSuspended());
            assertEquals(3, stats.getAborted());
            assertEquals(1, stats.getStale());
            assertEquals(8, stats.getPendingWorkItems());
            assertEquals(12, stats.getAvailableModels());
        }

        @Test
        @DisplayName("WorkflowStats getTotal should sum all instance counts")
        void testWorkflowStatsTotal() {
            WorkflowStats stats = new WorkflowStats();
            stats.setRunning(10);
            stats.setCompleted(100);
            stats.setSuspended(5);
            stats.setAborted(3);
            stats.setStale(2);

            assertEquals(120, stats.getTotal());
        }

        @Test
        @DisplayName("WorkflowStats getTotal should return zero when no instances")
        void testWorkflowStatsTotalZero() {
            WorkflowStats stats = new WorkflowStats();

            assertEquals(0, stats.getTotal());
        }

        @Test
        @DisplayName("WorkflowStats toString should include all counts")
        void testWorkflowStatsToString() {
            WorkflowStats stats = new WorkflowStats();
            stats.setRunning(5);
            stats.setCompleted(150);
            stats.setSuspended(2);
            stats.setAborted(3);
            stats.setStale(1);
            stats.setPendingWorkItems(8);
            stats.setAvailableModels(12);

            String result = stats.toString();
            assertTrue(result.contains("5 running"));
            assertTrue(result.contains("150 completed"));
            assertTrue(result.contains("2 suspended"));
            assertTrue(result.contains("3 aborted"));
            assertTrue(result.contains("1 stale"));
            assertTrue(result.contains("8"));  // Work items
            assertTrue(result.contains("12")); // Models
        }
    }

    @Nested
    @DisplayName("WorkflowStatus Enum Tests")
    class WorkflowStatusTests {

        @Test
        @DisplayName("WorkflowStatus should have all expected values")
        void testWorkflowStatusValues() {
            WorkflowStatus[] statuses = WorkflowStatus.values();

            assertEquals(5, statuses.length);
            assertTrue(containsStatus(statuses, "RUNNING"));
            assertTrue(containsStatus(statuses, "COMPLETED"));
            assertTrue(containsStatus(statuses, "ABORTED"));
            assertTrue(containsStatus(statuses, "SUSPENDED"));
            assertTrue(containsStatus(statuses, "STALE"));
        }

        @Test
        @DisplayName("WorkflowStatus valueOf should work correctly")
        void testWorkflowStatusValueOf() {
            assertEquals(WorkflowStatus.RUNNING, WorkflowStatus.valueOf("RUNNING"));
            assertEquals(WorkflowStatus.COMPLETED, WorkflowStatus.valueOf("COMPLETED"));
            assertEquals(WorkflowStatus.ABORTED, WorkflowStatus.valueOf("ABORTED"));
            assertEquals(WorkflowStatus.SUSPENDED, WorkflowStatus.valueOf("SUSPENDED"));
            assertEquals(WorkflowStatus.STALE, WorkflowStatus.valueOf("STALE"));
        }

        @Test
        @DisplayName("WorkflowStatus valueOf should throw for invalid values")
        void testWorkflowStatusInvalid() {
            assertThrows(IllegalArgumentException.class, () ->
                WorkflowStatus.valueOf("INVALID")
            );
        }

        private boolean containsStatus(WorkflowStatus[] statuses, String name) {
            for (WorkflowStatus status : statuses) {
                if (status.name().equals(name)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Nested
    @DisplayName("Data Class Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("WorkflowModel should handle null values gracefully")
        void testWorkflowModelNullValues() {
            WorkflowModel model = new WorkflowModel();
            // Should not throw NPE
            assertNull(model.getPath());
            assertNull(model.getId());
            assertNull(model.getTitle());
            assertNull(model.getDescription());
            assertNull(model.getVersion());
            assertFalse(model.isTransient());
        }

        @Test
        @DisplayName("WorkflowInstance should handle null values gracefully")
        void testWorkflowInstanceNullValues() {
            WorkflowInstance instance = new WorkflowInstance();
            // Should not throw NPE
            assertNull(instance.getPath());
            assertNull(instance.getId());
            assertNull(instance.getModelId());
            assertNull(instance.getModelTitle());
            assertNull(instance.getPayload());
            assertNull(instance.getStatus());
            assertNull(instance.getInitiator());
            assertNull(instance.getStartTime());
            assertNull(instance.getEndTime());
            assertNull(instance.getCurrentStep());
        }

        @Test
        @DisplayName("WorkItem should handle null values gracefully")
        void testWorkItemNullValues() {
            WorkItem item = new WorkItem();
            // Should not throw NPE
            assertNull(item.getId());
            assertNull(item.getWorkflowId());
            assertNull(item.getTitle());
            assertNull(item.getPayload());
            assertNull(item.getAssignee());
            assertNull(item.getStepTitle());
            assertNull(item.getStartTime());
            assertNull(item.getDueTime());
            assertNull(item.getComment());
        }

        @Test
        @DisplayName("WorkflowHistoryItem should handle null values gracefully")
        void testWorkflowHistoryItemNullValues() {
            WorkflowHistoryItem item = new WorkflowHistoryItem();
            // Should not throw NPE
            assertNull(item.getWorkflowId());
            assertNull(item.getModelTitle());
            assertNull(item.getStatus());
            assertNull(item.getInitiator());
            assertNull(item.getStartTime());
            assertNull(item.getEndTime());
        }

        @Test
        @DisplayName("Data classes should handle empty strings")
        void testEmptyStrings() {
            WorkflowModel model = new WorkflowModel();
            model.setTitle("");
            model.setDescription("");

            assertEquals("", model.getTitle());
            assertEquals("", model.getDescription());
        }
    }
}
