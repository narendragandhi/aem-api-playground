package com.aemtools.aem.api;

import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API client for AEM Workflow operations.
 * Supports listing workflow models, starting/stopping workflows,
 * managing workflow instances, and monitoring workflow status.
 */
public class WorkflowApi {

    private final AemApiClient client;
    private final ObjectMapper mapper;

    // AEM Workflow API endpoints
    private static final String WORKFLOW_INSTANCES_PATH = "/etc/workflow/instances";
    private static final String WORKFLOW_MODELS_PATH = "/var/workflow/models";
    private static final String WORKFLOW_CONSOLE_PATH = "/libs/cq/workflow/admin/console/content";
    private static final String WORKFLOW_API_PATH = "/api/workflow";

    public WorkflowApi(AemApiClient client) {
        this.client = client;
        this.mapper = client.getObjectMapper();
    }

    // ========================================
    // Workflow Models
    // ========================================

    /**
     * Lists all available workflow models.
     *
     * @return list of workflow models
     * @throws IOException if API call fails
     */
    public List<WorkflowModel> listModels() throws IOException {
        List<WorkflowModel> models = new ArrayList<>();

        JsonNode response = client.get(WORKFLOW_MODELS_PATH + ".json");

        if (response.isObject()) {
            response.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode node = entry.getValue();

                // Skip jcr: properties
                if (!key.startsWith("jcr:") && node.isObject()) {
                    WorkflowModel model = parseWorkflowModel(node, WORKFLOW_MODELS_PATH + "/" + key);
                    if (model.getTitle() != null && !model.getTitle().isEmpty()) {
                        models.add(model);
                    }
                }
            });
        }

        return models;
    }

    /**
     * Gets details of a specific workflow model.
     *
     * @param modelPath path to the workflow model
     * @return workflow model details
     * @throws IOException if API call fails
     */
    public WorkflowModel getModel(String modelPath) throws IOException {
        JsonNode response = client.get(modelPath + ".json");
        return parseWorkflowModel(response, modelPath);
    }

    // ========================================
    // Workflow Instances
    // ========================================

    /**
     * Lists workflow instances with optional filters.
     *
     * @param status filter by status (RUNNING, COMPLETED, ABORTED, SUSPENDED, STALE)
     * @param limit maximum number of results
     * @return list of workflow instances
     * @throws IOException if API call fails
     */
    public List<WorkflowInstance> listInstances(WorkflowStatus status, int limit) throws IOException {
        List<WorkflowInstance> instances = new ArrayList<>();

        StringBuilder path = new StringBuilder(WORKFLOW_INSTANCES_PATH);
        if (status != null) {
            path.append("/server0/").append(status.name().toLowerCase());
        }
        path.append(".json");

        JsonNode response = client.get(path.toString());

        if (response.isObject()) {
            response.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode node = entry.getValue();

                if (!key.startsWith("jcr:") && node.isObject() && instances.size() < limit) {
                    WorkflowInstance instance = parseWorkflowInstance(node, WORKFLOW_INSTANCES_PATH + "/" + key);
                    if (instance.getId() != null) {
                        instances.add(instance);
                    }
                }
            });
        } else if (response.isArray()) {
            for (JsonNode node : response) {
                if (instances.size() >= limit) break;
                WorkflowInstance instance = parseWorkflowInstance(node, null);
                if (instance.getId() != null) {
                    instances.add(instance);
                }
            }
        }

        return instances;
    }

    /**
     * Lists all running workflow instances.
     *
     * @param limit maximum number of results
     * @return list of running workflow instances
     * @throws IOException if API call fails
     */
    public List<WorkflowInstance> listRunningInstances(int limit) throws IOException {
        return listInstances(WorkflowStatus.RUNNING, limit);
    }

    /**
     * Gets details of a specific workflow instance.
     *
     * @param instanceId workflow instance ID
     * @return workflow instance details
     * @throws IOException if API call fails
     */
    public WorkflowInstance getInstance(String instanceId) throws IOException {
        String path = instanceId.startsWith("/") ? instanceId : WORKFLOW_INSTANCES_PATH + "/" + instanceId;
        JsonNode response = client.get(path + ".json");
        return parseWorkflowInstance(response, path);
    }

    // ========================================
    // Workflow Operations
    // ========================================

    /**
     * Starts a new workflow instance.
     *
     * @param modelPath path to the workflow model (e.g., /var/workflow/models/request_for_activation)
     * @param payloadPath path to the content payload (e.g., /content/dam/myasset.jpg)
     * @return the started workflow instance
     * @throws IOException if API call fails
     */
    public WorkflowInstance startWorkflow(String modelPath, String payloadPath) throws IOException {
        return startWorkflow(modelPath, payloadPath, "JCR_PATH", null);
    }

    /**
     * Starts a new workflow instance with metadata.
     *
     * @param modelPath path to the workflow model
     * @param payloadPath path to the content payload
     * @param payloadType type of payload (JCR_PATH, JCR_UUID, URL)
     * @param metadata optional workflow metadata
     * @return the started workflow instance
     * @throws IOException if API call fails
     */
    public WorkflowInstance startWorkflow(String modelPath, String payloadPath,
            String payloadType, Map<String, String> metadata) throws IOException {

        ObjectNode request = mapper.createObjectNode();
        request.put("model", modelPath);
        request.put("payload", payloadPath);
        request.put("payloadType", payloadType != null ? payloadType : "JCR_PATH");

        if (metadata != null && !metadata.isEmpty()) {
            ObjectNode metadataNode = mapper.createObjectNode();
            metadata.forEach(metadataNode::put);
            request.set("metaData", metadataNode);
        }

        // Try the modern API endpoint first
        try {
            JsonNode response = client.post("/api/workflow/instances", request);
            return parseWorkflowInstance(response, null);
        } catch (IOException e) {
            // Fall back to classic endpoint
            JsonNode response = client.post("/etc/workflow/instances.json", request);
            return parseWorkflowInstance(response, null);
        }
    }

    /**
     * Terminates/aborts a running workflow instance.
     *
     * @param instanceId workflow instance ID or path
     * @return true if successful
     * @throws IOException if API call fails
     */
    public boolean terminateWorkflow(String instanceId) throws IOException {
        String path = instanceId.startsWith("/") ? instanceId : WORKFLOW_INSTANCES_PATH + "/" + instanceId;

        ObjectNode request = mapper.createObjectNode();
        request.put("action", "terminate");

        try {
            client.post(path, request);
            return true;
        } catch (IOException e) {
            // Try alternative endpoint
            return client.delete(path);
        }
    }

    /**
     * Suspends a running workflow instance.
     *
     * @param instanceId workflow instance ID or path
     * @return true if successful
     * @throws IOException if API call fails
     */
    public boolean suspendWorkflow(String instanceId) throws IOException {
        String path = instanceId.startsWith("/") ? instanceId : WORKFLOW_INSTANCES_PATH + "/" + instanceId;

        ObjectNode request = mapper.createObjectNode();
        request.put("action", "suspend");

        client.post(path, request);
        return true;
    }

    /**
     * Resumes a suspended workflow instance.
     *
     * @param instanceId workflow instance ID or path
     * @return true if successful
     * @throws IOException if API call fails
     */
    public boolean resumeWorkflow(String instanceId) throws IOException {
        String path = instanceId.startsWith("/") ? instanceId : WORKFLOW_INSTANCES_PATH + "/" + instanceId;

        ObjectNode request = mapper.createObjectNode();
        request.put("action", "resume");

        client.post(path, request);
        return true;
    }

    /**
     * Retries a failed workflow step.
     *
     * @param instanceId workflow instance ID or path
     * @return true if successful
     * @throws IOException if API call fails
     */
    public boolean retryWorkflow(String instanceId) throws IOException {
        String path = instanceId.startsWith("/") ? instanceId : WORKFLOW_INSTANCES_PATH + "/" + instanceId;

        ObjectNode request = mapper.createObjectNode();
        request.put("action", "retry");

        client.post(path, request);
        return true;
    }

    // ========================================
    // Work Items
    // ========================================

    /**
     * Lists work items (inbox items) for the current user.
     *
     * @param limit maximum number of results
     * @return list of work items
     * @throws IOException if API call fails
     */
    public List<WorkItem> listWorkItems(int limit) throws IOException {
        List<WorkItem> items = new ArrayList<>();

        JsonNode response = client.get("/bin/workflow/inbox.json?limit=" + limit);

        if (response.has("workflows") && response.get("workflows").isArray()) {
            for (JsonNode node : response.get("workflows")) {
                items.add(parseWorkItem(node));
            }
        } else if (response.isArray()) {
            for (JsonNode node : response) {
                if (items.size() >= limit) break;
                items.add(parseWorkItem(node));
            }
        }

        return items;
    }

    /**
     * Completes a work item (advances the workflow).
     *
     * @param workItemId work item ID
     * @param routeId route/transition to follow (null for default)
     * @param comment optional comment
     * @return true if successful
     * @throws IOException if API call fails
     */
    public boolean completeWorkItem(String workItemId, String routeId, String comment) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("item", workItemId);
        request.put("action", "complete");

        if (routeId != null) {
            request.put("route", routeId);
        }
        if (comment != null) {
            request.put("comment", comment);
        }

        client.post("/bin/workflow/inbox", request);
        return true;
    }

    /**
     * Delegates a work item to another user.
     *
     * @param workItemId work item ID
     * @param delegatee user ID to delegate to
     * @param comment optional comment
     * @return true if successful
     * @throws IOException if API call fails
     */
    public boolean delegateWorkItem(String workItemId, String delegatee, String comment) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("item", workItemId);
        request.put("action", "delegate");
        request.put("delegatee", delegatee);

        if (comment != null) {
            request.put("comment", comment);
        }

        client.post("/bin/workflow/inbox", request);
        return true;
    }

    /**
     * Steps back a work item to a previous step.
     *
     * @param workItemId work item ID
     * @param stepId step ID to go back to
     * @param comment optional comment
     * @return true if successful
     * @throws IOException if API call fails
     */
    public boolean stepBackWorkItem(String workItemId, String stepId, String comment) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("item", workItemId);
        request.put("action", "stepBack");
        request.put("step", stepId);

        if (comment != null) {
            request.put("comment", comment);
        }

        client.post("/bin/workflow/inbox", request);
        return true;
    }

    // ========================================
    // Workflow History
    // ========================================

    /**
     * Gets the workflow history for a content path.
     *
     * @param payloadPath content path
     * @return list of historical workflow instances
     * @throws IOException if API call fails
     */
    public List<WorkflowHistoryItem> getHistory(String payloadPath) throws IOException {
        List<WorkflowHistoryItem> history = new ArrayList<>();

        String encodedPath = payloadPath.replace("/", "%2F");
        JsonNode response = client.get("/bin/workflow/history.json?payload=" + encodedPath);

        if (response.isArray()) {
            for (JsonNode node : response) {
                history.add(parseHistoryItem(node));
            }
        } else if (response.has("history") && response.get("history").isArray()) {
            for (JsonNode node : response.get("history")) {
                history.add(parseHistoryItem(node));
            }
        }

        return history;
    }

    // ========================================
    // Bulk Operations
    // ========================================

    /**
     * Starts workflows for multiple payloads.
     *
     * @param modelPath workflow model path
     * @param payloadPaths list of content paths
     * @return map of payload path to result (instance ID or error message)
     * @throws IOException if API call fails
     */
    public Map<String, String> startBulkWorkflows(String modelPath, List<String> payloadPaths) throws IOException {
        Map<String, String> results = new HashMap<>();

        for (String payload : payloadPaths) {
            try {
                WorkflowInstance instance = startWorkflow(modelPath, payload);
                results.put(payload, instance.getId());
            } catch (Exception e) {
                results.put(payload, "ERROR: " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Terminates multiple workflow instances.
     *
     * @param instanceIds list of instance IDs
     * @return map of instance ID to success/failure
     */
    public Map<String, Boolean> terminateBulkWorkflows(List<String> instanceIds) {
        Map<String, Boolean> results = new HashMap<>();

        for (String instanceId : instanceIds) {
            try {
                boolean success = terminateWorkflow(instanceId);
                results.put(instanceId, success);
            } catch (Exception e) {
                results.put(instanceId, false);
            }
        }

        return results;
    }

    // ========================================
    // Purge Operations
    // ========================================

    /**
     * Purges completed workflow instances older than specified days.
     *
     * @param olderThanDays minimum age in days
     * @return number of purged instances
     * @throws IOException if API call fails
     */
    public int purgeCompletedWorkflows(int olderThanDays) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("cmd", "purge");
        request.put("status", "COMPLETED");
        request.put("olderThanDays", olderThanDays);

        JsonNode response = client.post("/etc/workflow/instances.purge.json", request);

        return response.path("purged").asInt(0);
    }

    // ========================================
    // Statistics
    // ========================================

    /**
     * Gets workflow statistics.
     *
     * @return workflow statistics
     * @throws IOException if API call fails
     */
    public WorkflowStats getStatistics() throws IOException {
        WorkflowStats stats = new WorkflowStats();

        // Count instances by status
        try {
            stats.setRunning(listInstances(WorkflowStatus.RUNNING, 1000).size());
            stats.setCompleted(listInstances(WorkflowStatus.COMPLETED, 1000).size());
            stats.setSuspended(listInstances(WorkflowStatus.SUSPENDED, 1000).size());
            stats.setAborted(listInstances(WorkflowStatus.ABORTED, 1000).size());
            stats.setStale(listInstances(WorkflowStatus.STALE, 1000).size());
        } catch (IOException e) {
            // Partial stats if some queries fail
        }

        // Count work items
        try {
            stats.setPendingWorkItems(listWorkItems(1000).size());
        } catch (IOException e) {
            // Skip if inbox not accessible
        }

        // Count models
        try {
            stats.setAvailableModels(listModels().size());
        } catch (IOException e) {
            // Skip if models not accessible
        }

        return stats;
    }

    // ========================================
    // Parsing Helpers
    // ========================================

    private WorkflowModel parseWorkflowModel(JsonNode node, String path) {
        WorkflowModel model = new WorkflowModel();
        model.setPath(path != null ? path : node.path("path").asText());
        model.setId(node.path("modelId").asText(node.path("id").asText()));
        model.setTitle(node.path("jcr:title").asText(node.path("title").asText()));
        model.setDescription(node.path("jcr:description").asText(node.path("description").asText()));
        model.setVersion(node.path("version").asText("1.0"));
        model.setTransient(node.path("transient").asBoolean(false));
        return model;
    }

    private WorkflowInstance parseWorkflowInstance(JsonNode node, String path) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setPath(path != null ? path : node.path("path").asText());
        instance.setId(node.path("id").asText(node.path("workflowId").asText()));
        instance.setModelId(node.path("modelId").asText(node.path("model").asText()));
        instance.setModelTitle(node.path("modelTitle").asText(node.path("modelName").asText()));
        instance.setPayload(node.path("payload").asText(node.path("payloadPath").asText()));
        instance.setStatus(node.path("status").asText(node.path("state").asText()));
        instance.setInitiator(node.path("initiator").asText(node.path("startedBy").asText()));
        instance.setStartTime(node.path("startTime").asText(node.path("started").asText()));
        instance.setEndTime(node.path("endTime").asText(node.path("ended").asText()));
        instance.setCurrentStep(node.path("currentStep").asText(node.path("currentNodeTitle").asText()));
        return instance;
    }

    private WorkItem parseWorkItem(JsonNode node) {
        WorkItem item = new WorkItem();
        item.setId(node.path("id").asText(node.path("itemId").asText()));
        item.setWorkflowId(node.path("workflowId").asText());
        item.setTitle(node.path("title").asText(node.path("name").asText()));
        item.setPayload(node.path("payload").asText(node.path("payloadPath").asText()));
        item.setAssignee(node.path("assignee").asText(node.path("currentAssignee").asText()));
        item.setStepTitle(node.path("stepTitle").asText(node.path("currentStep").asText()));
        item.setStartTime(node.path("startTime").asText());
        item.setDueTime(node.path("dueTime").asText());
        item.setComment(node.path("comment").asText());
        return item;
    }

    private WorkflowHistoryItem parseHistoryItem(JsonNode node) {
        WorkflowHistoryItem item = new WorkflowHistoryItem();
        item.setWorkflowId(node.path("workflowId").asText());
        item.setModelTitle(node.path("modelTitle").asText());
        item.setStatus(node.path("status").asText());
        item.setInitiator(node.path("initiator").asText());
        item.setStartTime(node.path("startTime").asText());
        item.setEndTime(node.path("endTime").asText());
        return item;
    }

    // ========================================
    // Data Classes
    // ========================================

    /**
     * Workflow status enumeration.
     */
    public enum WorkflowStatus {
        RUNNING,
        COMPLETED,
        ABORTED,
        SUSPENDED,
        STALE
    }

    /**
     * Represents a workflow model definition.
     */
    public static class WorkflowModel {
        private String path;
        private String id;
        private String title;
        private String description;
        private String version;
        private boolean isTransient;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public boolean isTransient() { return isTransient; }
        public void setTransient(boolean isTransient) { this.isTransient = isTransient; }

        @Override
        public String toString() {
            return String.format("%s (%s) - %s", title, version, path);
        }
    }

    /**
     * Represents a running or completed workflow instance.
     */
    public static class WorkflowInstance {
        private String path;
        private String id;
        private String modelId;
        private String modelTitle;
        private String payload;
        private String status;
        private String initiator;
        private String startTime;
        private String endTime;
        private String currentStep;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getModelId() { return modelId; }
        public void setModelId(String modelId) { this.modelId = modelId; }
        public String getModelTitle() { return modelTitle; }
        public void setModelTitle(String modelTitle) { this.modelTitle = modelTitle; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getInitiator() { return initiator; }
        public void setInitiator(String initiator) { this.initiator = initiator; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public String getCurrentStep() { return currentStep; }
        public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }

        @Override
        public String toString() {
            return String.format("%s - %s [%s] (%s)", id, modelTitle, status, payload);
        }
    }

    /**
     * Represents a work item in the user's inbox.
     */
    public static class WorkItem {
        private String id;
        private String workflowId;
        private String title;
        private String payload;
        private String assignee;
        private String stepTitle;
        private String startTime;
        private String dueTime;
        private String comment;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getWorkflowId() { return workflowId; }
        public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }
        public String getStepTitle() { return stepTitle; }
        public void setStepTitle(String stepTitle) { this.stepTitle = stepTitle; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getDueTime() { return dueTime; }
        public void setDueTime(String dueTime) { this.dueTime = dueTime; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }

        @Override
        public String toString() {
            return String.format("%s - %s [%s] (%s)", id, title, stepTitle, assignee);
        }
    }

    /**
     * Represents a historical workflow entry.
     */
    public static class WorkflowHistoryItem {
        private String workflowId;
        private String modelTitle;
        private String status;
        private String initiator;
        private String startTime;
        private String endTime;

        public String getWorkflowId() { return workflowId; }
        public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
        public String getModelTitle() { return modelTitle; }
        public void setModelTitle(String modelTitle) { this.modelTitle = modelTitle; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getInitiator() { return initiator; }
        public void setInitiator(String initiator) { this.initiator = initiator; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }

        @Override
        public String toString() {
            return String.format("%s - %s [%s] by %s", workflowId, modelTitle, status, initiator);
        }
    }

    /**
     * Workflow statistics summary.
     */
    public static class WorkflowStats {
        private int running;
        private int completed;
        private int suspended;
        private int aborted;
        private int stale;
        private int pendingWorkItems;
        private int availableModels;

        public int getRunning() { return running; }
        public void setRunning(int running) { this.running = running; }
        public int getCompleted() { return completed; }
        public void setCompleted(int completed) { this.completed = completed; }
        public int getSuspended() { return suspended; }
        public void setSuspended(int suspended) { this.suspended = suspended; }
        public int getAborted() { return aborted; }
        public void setAborted(int aborted) { this.aborted = aborted; }
        public int getStale() { return stale; }
        public void setStale(int stale) { this.stale = stale; }
        public int getPendingWorkItems() { return pendingWorkItems; }
        public void setPendingWorkItems(int pendingWorkItems) { this.pendingWorkItems = pendingWorkItems; }
        public int getAvailableModels() { return availableModels; }
        public void setAvailableModels(int availableModels) { this.availableModels = availableModels; }

        public int getTotal() {
            return running + completed + suspended + aborted + stale;
        }

        @Override
        public String toString() {
            return String.format(
                "Workflows: %d running, %d completed, %d suspended, %d aborted, %d stale | Work Items: %d | Models: %d",
                running, completed, suspended, aborted, stale, pendingWorkItems, availableModels
            );
        }
    }
}
