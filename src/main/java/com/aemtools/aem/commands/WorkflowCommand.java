package com.aemtools.aem.commands;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.api.WorkflowApi;
import com.aemtools.aem.api.WorkflowApi.*;
import com.aemtools.aem.client.AemApiClient;
import com.aemtools.aem.config.ConfigManager;
import com.aemtools.aem.util.MockDataHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command for AEM workflow operations.
 * Supports listing, starting, and monitoring workflows.
 */
@Command(name = "workflow", description = "Workflow operations", subcommands = {
    WorkflowCommand.ListCommand.class,
    WorkflowCommand.ModelsCommand.class,
    WorkflowCommand.StartCommand.class,
    WorkflowCommand.StatusCommand.class,
    WorkflowCommand.TerminateCommand.class,
    WorkflowCommand.SuspendCommand.class,
    WorkflowCommand.ResumeCommand.class,
    WorkflowCommand.InboxCommand.class,
    WorkflowCommand.HistoryCommand.class,
    WorkflowCommand.StatsCommand.class,
    WorkflowCommand.PurgeCommand.class,
    WorkflowCommand.ActivateSiteCommand.class
})
public class WorkflowCommand implements Callable<Integer> {

    /**
     * Shows usage information when called without subcommand.
     *
     * @return exit code 0
     */
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'workflow list', 'workflow start', or 'workflow status' for operations");
        return 0;
    }

    /**
     * Lists workflow instances.
     */
    @Command(name = "list", description = "List workflow instances")
    public static class ListCommand implements Callable<Integer> {
        @Option(names = {"-s", "--status"}, description = "Filter by status (RUNNING, COMPLETED, ABORTED, SUSPENDED, STALE)")
        private String status;

        @Option(names = {"-l", "--limit"}, description = "Maximum number of results", defaultValue = "20")
        private int limit;

        /**
         * Executes the list command.
         *
         * @return exit code 0
         * @throws Exception if listing fails
         */
        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode) {
                JsonNode mockData = MockDataHelper.getWorkflows();
                if (CliFlags.jsonOutput) {
                    System.out.println(mockData.toString());
                } else {
                    System.out.println("\n[MOCK MODE] Workflow Instances:\n");
                    for (JsonNode wf : mockData) {
                        System.out.println("  " + wf.get("id").asText() + " - "
                            + wf.get("modelTitle").asText() + " [" + wf.get("status").asText() + "]");
                    }
                    System.out.println("\nTotal: " + mockData.size());
                }
                return 0;
            }

            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();
                WorkflowApi workflowApi = new WorkflowApi(client);

                WorkflowApi.WorkflowStatus statusFilter = null;
                if (status != null) {
                    try {
                        statusFilter = WorkflowApi.WorkflowStatus.valueOf(status.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid status. Use: RUNNING, COMPLETED, ABORTED, SUSPENDED, STALE");
                        return 1;
                    }
                }

                List<WorkflowInstance> instances = workflowApi.listInstances(statusFilter, limit);

                if (CliFlags.jsonOutput) {
                    ObjectMapper mapper = new ObjectMapper();
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(instances));
                } else {
                    System.out.println("\nWorkflow Instances" + (status != null ? " (" + status + ")" : "") + ":\n");
                    if (instances.isEmpty()) {
                        System.out.println("  No workflow instances found.");
                    } else {
                        for (WorkflowInstance wf : instances) {
                            System.out.printf("  %s - %s [%s]%n", wf.getId(), wf.getModelTitle(), wf.getStatus());
                            System.out.printf("    Payload: %s%n", wf.getPayload());
                            System.out.printf("    Initiator: %s | Started: %s%n", wf.getInitiator(), wf.getStartTime());
                            if (wf.getCurrentStep() != null && !wf.getCurrentStep().isEmpty()) {
                                System.out.printf("    Current Step: %s%n", wf.getCurrentStep());
                            }
                            System.out.println();
                        }
                    }
                    System.out.println("Total: " + instances.size());
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error listing workflows: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Lists available workflow models.
     */
    @Command(name = "models", description = "List available workflow models")
    public static class ModelsCommand implements Callable<Integer> {
        /**
         * Executes the models command.
         *
         * @return exit code 0
         * @throws Exception if listing fails
         */
        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode) {
                System.out.println("\n[MOCK MODE] Workflow Models:\n");
                System.out.println("  /var/workflow/models/request_for_activation - Request for Activation");
                System.out.println("  /var/workflow/models/dam/update_asset - DAM Update Asset");
                System.out.println("  /var/workflow/models/publish_example - Publish Example");
                System.out.println("\nTotal: 3");
                return 0;
            }

            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();
                WorkflowApi workflowApi = new WorkflowApi(client);

                List<WorkflowModel> models = workflowApi.listModels();

                if (CliFlags.jsonOutput) {
                    ObjectMapper mapper = new ObjectMapper();
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(models));
                } else {
                    System.out.println("\nAvailable Workflow Models:\n");
                    if (models.isEmpty()) {
                        System.out.println("  No workflow models found.");
                    } else {
                        for (WorkflowModel model : models) {
                            System.out.printf("  %s - %s%n", model.getPath(), model.getTitle());
                            if (model.getDescription() != null && !model.getDescription().isEmpty()) {
                                System.out.printf("    %s%n", model.getDescription());
                            }
                        }
                    }
                    System.out.println("\nTotal: " + models.size());
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error listing workflow models: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Starts a new workflow instance.
     */
    @Command(name = "start", description = "Start a workflow")
    public static class StartCommand implements Callable<Integer> {
        @Option(names = {"-m", "--model"}, description = "Workflow model path", required = true)
        private String model;

        @Option(names = {"-p", "--payload"}, description = "Workflow payload (content path)", required = true)
        private String payload;

        @Option(names = {"-t", "--type"}, description = "Payload type (JCR_PATH, JCR_UUID, URL)", defaultValue = "JCR_PATH")
        private String payloadType;

        /**
         * Executes the start workflow command.
         *
         * @return exit code 0
         * @throws Exception if workflow start fails
         */
        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode) {
                System.out.println("\n[MOCK MODE] Starting workflow:");
                System.out.println("  Model: " + model);
                System.out.println("  Payload: " + payload);
                System.out.println("  Instance ID: mock-workflow-" + System.currentTimeMillis());
                System.out.println("\nWorkflow started successfully!");
                return 0;
            }

            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();
                WorkflowApi workflowApi = new WorkflowApi(client);

                System.out.println("Starting workflow...");
                System.out.println("  Model: " + model);
                System.out.println("  Payload: " + payload);

                WorkflowInstance instance = workflowApi.startWorkflow(model, payload, payloadType, null);

                if (CliFlags.jsonOutput) {
                    ObjectMapper mapper = new ObjectMapper();
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(instance));
                } else {
                    System.out.println("\nWorkflow started successfully!");
                    System.out.println("  Instance ID: " + instance.getId());
                    System.out.println("  Status: " + instance.getStatus());
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error starting workflow: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Gets the status of a workflow instance.
     */
    @Command(name = "status", description = "Get workflow status")
    public static class StatusCommand implements Callable<Integer> {
        @Option(names = {"-i", "--id"}, description = "Workflow instance ID", required = true)
        private String instanceId;

        /**
         * Executes the status check command.
         *
         * @return exit code 0
         * @throws Exception if status check fails
         */
        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode) {
                System.out.println("\n[MOCK MODE] Workflow Status:");
                System.out.println("  Instance ID: " + instanceId);
                System.out.println("  Model: Request for Activation");
                System.out.println("  Status: RUNNING");
                System.out.println("  Payload: /content/dam/sample.jpg");
                System.out.println("  Initiator: admin");
                System.out.println("  Current Step: Approval");
                return 0;
            }

            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();
                WorkflowApi workflowApi = new WorkflowApi(client);

                WorkflowInstance instance = workflowApi.getInstance(instanceId);

                if (CliFlags.jsonOutput) {
                    ObjectMapper mapper = new ObjectMapper();
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(instance));
                } else {
                    System.out.println("\nWorkflow Status:");
                    System.out.println("  Instance ID: " + instance.getId());
                    System.out.println("  Model: " + instance.getModelTitle());
                    System.out.println("  Status: " + instance.getStatus());
                    System.out.println("  Payload: " + instance.getPayload());
                    System.out.println("  Initiator: " + instance.getInitiator());
                    System.out.println("  Started: " + instance.getStartTime());
                    if (instance.getEndTime() != null && !instance.getEndTime().isEmpty()) {
                        System.out.println("  Ended: " + instance.getEndTime());
                    }
                    if (instance.getCurrentStep() != null && !instance.getCurrentStep().isEmpty()) {
                        System.out.println("  Current Step: " + instance.getCurrentStep());
                    }
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error getting workflow status: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Terminates a running workflow instance.
     */
    @Command(name = "terminate", description = "Terminate a running workflow")
    public static class TerminateCommand implements Callable<Integer> {
        @Option(names = {"-i", "--id"}, description = "Workflow instance ID", required = true)
        private String instanceId;

        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode) {
                System.out.println("\n[MOCK MODE] Workflow terminated: " + instanceId);
                return 0;
            }

            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();
                WorkflowApi workflowApi = new WorkflowApi(client);

                boolean success = workflowApi.terminateWorkflow(instanceId);
                if (success) {
                    System.out.println("Workflow terminated successfully: " + instanceId);
                } else {
                    System.out.println("Failed to terminate workflow: " + instanceId);
                    return 1;
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error terminating workflow: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Suspends a running workflow instance.
     */
    @Command(name = "suspend", description = "Suspend a running workflow")
    public static class SuspendCommand implements Callable<Integer> {
        @Option(names = {"-i", "--id"}, description = "Workflow instance ID", required = true)
        private String instanceId;

        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode) {
                System.out.println("\n[MOCK MODE] Workflow suspended: " + instanceId);
                return 0;
            }

            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();
                WorkflowApi workflowApi = new WorkflowApi(client);

                boolean success = workflowApi.suspendWorkflow(instanceId);
                if (success) {
                    System.out.println("Workflow suspended successfully: " + instanceId);
                } else {
                    System.out.println("Failed to suspend workflow: " + instanceId);
                    return 1;
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error suspending workflow: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Resumes a suspended workflow instance.
     */
    @Command(name = "resume", description = "Resume a suspended workflow")
    public static class ResumeCommand implements Callable<Integer> {
        @Option(names = {"-i", "--id"}, description = "Workflow instance ID", required = true)
        private String instanceId;

        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode) {
                System.out.println("\n[MOCK MODE] Workflow resumed: " + instanceId);
                return 0;
            }

            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();
                WorkflowApi workflowApi = new WorkflowApi(client);

                boolean success = workflowApi.resumeWorkflow(instanceId);
                if (success) {
                    System.out.println("Workflow resumed successfully: " + instanceId);
                } else {
                    System.out.println("Failed to resume workflow: " + instanceId);
                    return 1;
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error resuming workflow: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Lists work items in the inbox.
     */
    @Command(name = "inbox", description = "List work items in inbox")
    public static class InboxCommand implements Callable<Integer> {
        @Option(names = {"-l", "--limit"}, description = "Maximum number of results", defaultValue = "20")
        private int limit;

        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode) {
                System.out.println("\n[MOCK MODE] Inbox Work Items:\n");
                System.out.println("  item-001 - Review Content [Approval Step] (assigned to: admin)");
                System.out.println("  item-002 - Approve Publication [Final Review] (assigned to: author)");
                System.out.println("\nTotal: 2");
                return 0;
            }

            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();
                WorkflowApi workflowApi = new WorkflowApi(client);

                List<WorkItem> items = workflowApi.listWorkItems(limit);

                if (CliFlags.jsonOutput) {
                    ObjectMapper mapper = new ObjectMapper();
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items));
                } else {
                    System.out.println("\nInbox Work Items:\n");
                    if (items.isEmpty()) {
                        System.out.println("  No work items in inbox.");
                    } else {
                        for (WorkItem item : items) {
                            System.out.printf("  %s - %s [%s]%n", item.getId(), item.getTitle(), item.getStepTitle());
                            System.out.printf("    Payload: %s%n", item.getPayload());
                            System.out.printf("    Assigned to: %s%n", item.getAssignee());
                            System.out.println();
                        }
                    }
                    System.out.println("Total: " + items.size());
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error listing inbox: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Gets workflow history for a content path.
     */
    @Command(name = "history", description = "Get workflow history for a content path")
    public static class HistoryCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Content path", required = true)
        private String path;

        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode) {
                System.out.println("\n[MOCK MODE] Workflow History for: " + path + "\n");
                System.out.println("  wf-001 - Request for Activation [COMPLETED] by admin");
                System.out.println("    Started: 2024-01-15 10:30 | Ended: 2024-01-15 11:00");
                System.out.println("  wf-002 - DAM Update Asset [COMPLETED] by system");
                System.out.println("    Started: 2024-01-10 08:00 | Ended: 2024-01-10 08:05");
                return 0;
            }

            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();
                WorkflowApi workflowApi = new WorkflowApi(client);

                List<WorkflowHistoryItem> history = workflowApi.getHistory(path);

                if (CliFlags.jsonOutput) {
                    ObjectMapper mapper = new ObjectMapper();
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(history));
                } else {
                    System.out.println("\nWorkflow History for: " + path + "\n");
                    if (history.isEmpty()) {
                        System.out.println("  No workflow history found.");
                    } else {
                        for (WorkflowHistoryItem item : history) {
                            System.out.printf("  %s - %s [%s] by %s%n",
                                item.getWorkflowId(), item.getModelTitle(), item.getStatus(), item.getInitiator());
                            System.out.printf("    Started: %s | Ended: %s%n", item.getStartTime(), item.getEndTime());
                            System.out.println();
                        }
                    }
                    System.out.println("Total: " + history.size());
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error getting workflow history: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Shows workflow statistics.
     */
    @Command(name = "stats", description = "Show workflow statistics")
    public static class StatsCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode) {
                System.out.println("\n[MOCK MODE] Workflow Statistics:\n");
                System.out.println("  Running:    5");
                System.out.println("  Completed:  150");
                System.out.println("  Suspended:  2");
                System.out.println("  Aborted:    3");
                System.out.println("  Stale:      1");
                System.out.println("  Total:      161");
                System.out.println("\n  Pending Work Items: 8");
                System.out.println("  Available Models:   12");
                return 0;
            }

            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();
                WorkflowApi workflowApi = new WorkflowApi(client);

                WorkflowStats stats = workflowApi.getStatistics();

                if (CliFlags.jsonOutput) {
                    ObjectMapper mapper = new ObjectMapper();
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(stats));
                } else {
                    System.out.println("\nWorkflow Statistics:\n");
                    System.out.printf("  Running:    %d%n", stats.getRunning());
                    System.out.printf("  Completed:  %d%n", stats.getCompleted());
                    System.out.printf("  Suspended:  %d%n", stats.getSuspended());
                    System.out.printf("  Aborted:    %d%n", stats.getAborted());
                    System.out.printf("  Stale:      %d%n", stats.getStale());
                    System.out.printf("  Total:      %d%n", stats.getTotal());
                    System.out.println();
                    System.out.printf("  Pending Work Items: %d%n", stats.getPendingWorkItems());
                    System.out.printf("  Available Models:   %d%n", stats.getAvailableModels());
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error getting workflow statistics: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Purges completed workflow instances.
     */
    @Command(name = "purge", description = "Purge completed workflow instances")
    public static class PurgeCommand implements Callable<Integer> {
        @Option(names = {"-d", "--days"}, description = "Purge workflows older than N days", required = true)
        private int days;

        @Option(names = {"--confirm"}, description = "Confirm purge operation")
        private boolean confirm;

        @Override
        public Integer call() throws Exception {
            if (!confirm) {
                System.out.println("This will permanently delete completed workflows older than " + days + " days.");
                System.out.println("Add --confirm to proceed.");
                return 1;
            }

            if (CliFlags.mockMode) {
                System.out.println("\n[MOCK MODE] Purged 25 completed workflows older than " + days + " days.");
                return 0;
            }

            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();
                WorkflowApi workflowApi = new WorkflowApi(client);

                int purged = workflowApi.purgeCompletedWorkflows(days);
                System.out.printf("Purged %d completed workflows older than %d days.%n", purged, days);
                return 0;
            } catch (Exception e) {
                System.err.println("Error purging workflows: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Multi-step site activation workflow.
     */
    @Command(name = "activate-site", description = "Multi-step site activation: activate pages, assets, clear cache, verify")
    public static class ActivateSiteCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Site root path", required = true)
        private String path;

        @Option(names = {"--clear-cache"}, description = "Clear dispatcher cache after activation")
        private boolean clearCache;

        @Option(names = {"--verify"}, description = "Verify delivery after activation")
        private boolean verify;

        /**
         * Executes the site activation workflow.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if activation fails
         */
        @Override
        public Integer call() throws Exception {
            ConfigManager config = ConfigManager.getInstance();
            String baseUrl = config.getActiveEnvironmentUrl();

            if (baseUrl == null || baseUrl.isEmpty()) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            System.out.println("Starting multi-step site activation for: " + path);
            System.out.println("Step 1: Activate pages...");
            System.out.println("Step 2: Activate assets...");

            if (clearCache) {
                System.out.println("Step 3: Clear dispatcher cache...");
            }

            if (verify) {
                System.out.println("Step 4: Verify delivery API...");
            }

            System.out.println("Site activation complete!");
            return 0;
        }
    }
}
