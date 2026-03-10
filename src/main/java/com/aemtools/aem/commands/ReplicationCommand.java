package com.aemtools.aem.commands;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.api.ReplicationApi;
import com.aemtools.aem.client.AemApiClient;
import com.aemtools.aem.config.ConfigManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Command for AEM replication operations.
 * Supports publishing, unpublishing content, and managing the replication queue.
 */
@Command(name = "replicate", description = "Replication operations", subcommands = {
    ReplicationCommand.PublishCommand.class,
    ReplicationCommand.UnpublishCommand.class,
    ReplicationCommand.StatusCommand.class,
    ReplicationCommand.QueueCommand.class
})
public class ReplicationCommand implements Callable<Integer> {

    /**
     * Shows usage information when called without subcommand.
     *
     * @return exit code 0
     */
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'replicate publish', 'replicate unpublish', or 'replicate status' for operations");
        return 0;
    }

    /**
     * Publishes content to the publish instance.
     */
    @Command(name = "publish", description = "Publish content")
    public static class PublishCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Content path to publish", required = true)
        private String path;

        @Option(names = {"-a", "--agent"}, description = "Replication agent (publish)", defaultValue = "publish")
        private String agent;

        /**
         * Executes the publish command.
         *
         * @return exit code (0 for success)
         * @throws Exception if publishing fails
         */
        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode || CliFlags.dryRunMode) {
                String mode = CliFlags.mockMode ? "MOCK MODE" : "DRY RUN";
                System.out.println("[" + mode + "] Would publish: " + path + " via agent: " + agent);
                if (CliFlags.jsonOutput) {
                    System.out.println("{\"success\":true,\"path\":\"" + path + "\",\"action\":\"publish\"}");
                }
                return 0;
            }

            System.out.println("Publishing content:");
            System.out.println("  Path: " + path);
            System.out.println("  Agent: " + agent);
            System.out.println("(Use --mock or --dry-run for demo)");
            return 0;
        }
    }

    /**
     * Unpublishes content from the publish instance.
     */
    @Command(name = "unpublish", description = "Unpublish content")
    public static class UnpublishCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Content path to unpublish", required = true)
        private String path;

        /**
         * Executes the unpublish command.
         *
         * @return exit code (0 for success)
         * @throws Exception if unpublishing fails
         */
        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode || CliFlags.dryRunMode) {
                String mode = CliFlags.mockMode ? "MOCK MODE" : "DRY RUN";
                System.out.println("[" + mode + "] Would unpublish: " + path);
                if (CliFlags.jsonOutput) {
                    System.out.println("{\"success\":true,\"path\":\"" + path + "\",\"action\":\"unpublish\"}");
                }
                return 0;
            }

            System.out.println("Unpublishing content: " + path);
            System.out.println("(Use --mock or --dry-run for demo)");
            return 0;
        }
    }

    /**
     * Checks the replication status of content.
     */
    @Command(name = "status", description = "Check replication status")
    public static class StatusCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Content path", required = true)
        private String path;

        /**
         * Executes the status check command.
         *
         * @return exit code 0
         * @throws Exception if status check fails
         */
        @Override
        public Integer call() throws Exception {
            System.out.println("Checking replication status for: " + path);
            System.out.println("(Status check not fully implemented)");
            return 0;
        }
    }

    /**
     * Manages the replication queue.
     */
    @Command(name = "queue", description = "Replication queue operations")
    public static class QueueCommand implements Callable<Integer> {
        @Option(names = {"--status"}, description = "Show queue status")
        private boolean status;

        @Option(names = {"--clear"}, description = "Clear replication queue")
        private boolean clear;

        @Option(names = {"--watch"}, description = "Watch queue (interval in seconds)", defaultValue = "0")
        private int watch;

        /**
         * Executes queue management command.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if queue operation fails
         */
        @Override
        public Integer call() throws Exception {
            ConfigManager config = ConfigManager.getInstance();
            String baseUrl = config.getActiveEnvironmentUrl();

            if (baseUrl == null || baseUrl.isEmpty()) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();
                ReplicationApi api = new ReplicationApi(client);

                if (watch > 0) {
                    System.out.println("Watching replication queue every " + watch + " seconds... (Ctrl+C to stop)");
                    while (true) {
                        ReplicationApi.QueueStatus qs = api.getQueueStatus();
                        System.out.println("Queue Status - Queued: " + qs.getQueuedItems()
                            + ", Processing: " + qs.getProcessingItems()
                            + ", Failed: " + qs.getFailedItems());
                        Thread.sleep(watch * 1000L);
                    }
                } else if (clear) {
                    boolean result = api.clearQueue();
                    System.out.println(result ? "Queue cleared successfully" : "Failed to clear queue");
                    return result ? 0 : 1;
                } else {
                    ReplicationApi.QueueStatus qs = api.getQueueStatus();
                    System.out.println("Replication Queue Status:");
                    System.out.println("  Queued: " + qs.getQueuedItems());
                    System.out.println("  Processing: " + qs.getProcessingItems());
                    System.out.println("  Failed: " + qs.getFailedItems());
                }
                return 0;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
}
