package com.aemtools.aem.commands;

import com.aemtools.aem.audit.AuditLogger;
import com.aemtools.aem.audit.AuditLogger.*;
import com.aemtools.aem.client.AemApiClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command for viewing audit logs and managing API cache.
 * Provides visibility into API operations and performance.
 */
@Command(name = "audit", description = "View audit logs", subcommands = {
    AuditCommand.ListCommand.class,
    AuditCommand.StatsCommand.class,
    AuditCommand.PurgeCommand.class,
    AuditCommand.ClearCommand.class,
    AuditCommand.CacheCommand.class
})
public class AuditCommand implements Callable<Integer> {

    private static AemApiClient sharedClient;

    /**
     * Sets the shared API client for audit operations.
     *
     * @param client the AEM API client instance
     */
    public static void setSharedClient(AemApiClient client) {
        sharedClient = client;
    }

    /**
     * Shows usage information when called without subcommand.
     *
     * @return exit code 0
     */
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'audit list', 'audit clear', or 'audit cache' for operations");
        return 0;
    }

    /**
     * Lists audit log entries.
     */
    @Command(name = "list", description = "List audit log entries")
    public static class ListCommand implements Callable<Integer> {
        @Option(names = {"-m", "--max"}, description = "Max entries", defaultValue = "50")
        private int max;

        @Option(names = {"--api"}, description = "Show API calls")
        private boolean apiCalls;

        @Option(names = {"--actions"}, description = "Show user actions")
        private boolean userActions;

        @Option(names = {"--events"}, description = "Show system events")
        private boolean systemEvents;

        @Option(names = {"--memory"}, description = "Show in-memory log only")
        private boolean memoryOnly;

        /**
         * Executes the list audit entries command.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if listing fails
         */
        @Override
        public Integer call() throws Exception {
            AuditLogger auditLogger = AuditLogger.getInstance();

            // Default to showing API calls if no filter specified
            if (!apiCalls && !userActions && !systemEvents && !memoryOnly) {
                apiCalls = true;
            }

            if (memoryOnly) {
                if (sharedClient == null) {
                    System.out.println("No API client available. Run a command first to initialize.");
                    return 1;
                }
                Map<String, String> auditLog = sharedClient.getAuditLog();
                System.out.println("\nIn-Memory Audit Log (" + auditLog.size() + " entries):");
                int count = 0;
                for (Map.Entry<String, String> entry : auditLog.entrySet()) {
                    if (count++ >= max) break;
                    System.out.println("  " + entry.getValue());
                }
                return 0;
            }

            if (apiCalls) {
                List<ApiCallRecord> records = auditLogger.getRecentApiCalls(max);
                System.out.println("\n=== API Calls (" + records.size() + " entries) ===");
                for (ApiCallRecord record : records) {
                    String status = record.statusCode() >= 400 ? "[ERROR]" : "[OK]";
                    System.out.printf("  %s %s %s %s -> %d (%dms)%n",
                        record.timestamp().substring(0, 19),
                        status,
                        record.method(),
                        record.path(),
                        record.statusCode(),
                        record.durationMs());
                }
            }

            if (userActions) {
                List<UserActionRecord> records = auditLogger.getRecentUserActions(max);
                System.out.println("\n=== User Actions (" + records.size() + " entries) ===");
                for (UserActionRecord record : records) {
                    System.out.printf("  %s %s: %s -> %s%n",
                        record.timestamp().substring(0, 19),
                        record.action(),
                        record.target(),
                        record.result());
                }
            }

            if (systemEvents) {
                List<SystemEventRecord> records = auditLogger.getRecentSystemEvents(max);
                System.out.println("\n=== System Events (" + records.size() + " entries) ===");
                for (SystemEventRecord record : records) {
                    System.out.printf("  %s [%s] %s: %s%n",
                        record.timestamp().substring(0, 19),
                        record.severity(),
                        record.eventType(),
                        record.message());
                }
            }

            return 0;
        }
    }

    /**
     * Shows audit statistics.
     */
    @Command(name = "stats", description = "Show audit statistics")
    public static class StatsCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            AuditLogger auditLogger = AuditLogger.getInstance();
            AuditStats stats = auditLogger.getStats();

            System.out.println("\n=== Audit Statistics ===");
            System.out.printf("  Total API Calls:    %d%n", stats.getTotalApiCalls());
            System.out.printf("  API Calls Today:    %d%n", stats.getApiCallsToday());
            System.out.printf("  Error Count:        %d%n", stats.getErrorCount());
            System.out.printf("  Avg Response Time:  %.1f ms%n", stats.getAvgResponseTimeMs());
            System.out.printf("  User Actions:       %d%n", stats.getTotalUserActions());
            System.out.printf("  System Events:      %d%n", stats.getTotalSystemEvents());

            List<String> topEndpoints = stats.getTopEndpoints();
            if (!topEndpoints.isEmpty()) {
                System.out.println("\n  Top Endpoints:");
                for (String endpoint : topEndpoints) {
                    System.out.println("    - " + endpoint);
                }
            }

            return 0;
        }
    }

    /**
     * Purges old audit records.
     */
    @Command(name = "purge", description = "Purge old audit records")
    public static class PurgeCommand implements Callable<Integer> {
        @Option(names = {"-d", "--days"}, description = "Delete records older than N days", required = true)
        private int days;

        @Option(names = {"--confirm"}, description = "Confirm purge operation")
        private boolean confirm;

        @Override
        public Integer call() throws Exception {
            if (!confirm) {
                System.out.println("This will permanently delete audit records older than " + days + " days.");
                System.out.println("Add --confirm to proceed.");
                return 1;
            }

            AuditLogger auditLogger = AuditLogger.getInstance();
            int deleted = auditLogger.purgeOldRecords(days);
            System.out.printf("Purged %d audit records older than %d days.%n", deleted, days);
            return 0;
        }
    }

    /**
     * Clears the in-memory audit log.
     */
    @Command(name = "clear", description = "Clear in-memory audit log")
    public static class ClearCommand implements Callable<Integer> {

        /**
         * Executes the clear audit log command.
         *
         * @return exit code 0
         * @throws Exception if clearing fails
         */
        @Override
        public Integer call() throws Exception {
            System.out.println("In-memory audit log clears on restart.");
            System.out.println("Use 'audit purge --days N --confirm' to delete persistent records.");
            return 0;
        }
    }

    /**
     * Manages API response cache.
     */
    @Command(name = "cache", description = "API response cache operations")
    public static class CacheCommand implements Callable<Integer> {
        @Option(names = {"--stats"}, description = "Show cache statistics")
        private boolean stats;

        @Option(names = {"--clear"}, description = "Clear the cache")
        private boolean clear;

        @Option(names = {"--enable"}, description = "Enable caching")
        private boolean enable;

        @Option(names = {"--disable"}, description = "Disable caching")
        private boolean disable;

        /**
         * Executes cache management command.
         *
         * @return exit code 0
         * @throws Exception if cache operation fails
         */
        @Override
        public Integer call() throws Exception {
            if (sharedClient == null) {
                sharedClient = new AemApiClient();
            }

            if (stats) {
                System.out.println("\n=== API Cache Stats ===");
                Map<String, Object> cacheStats = sharedClient.getCacheStats();
                for (Map.Entry<String, Object> entry : cacheStats.entrySet()) {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }
                return 0;
            }

            if (clear) {
                sharedClient.clearCache();
                System.out.println("API cache cleared.");
                return 0;
            }

            if (enable) {
                sharedClient.setCacheEnabled(true);
                System.out.println("API caching enabled.");
                return 0;
            }

            if (disable) {
                sharedClient.setCacheEnabled(false);
                System.out.println("API caching disabled.");
                return 0;
            }

            System.out.println("Usage: audit cache --stats|--clear|--enable|--disable");
            return 0;
        }
    }
}
