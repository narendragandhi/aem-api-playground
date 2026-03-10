package com.aemtools.aem.commands;

import com.aemtools.aem.client.AemApiClient;
import com.aemtools.aem.config.ConfigManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Command for GraphQL operations.
 * Supports executing queries, persisted queries, and listing available queries.
 */
@Command(name = "graphql", description = "GraphQL operations", subcommands = {
    GraphQLCommand.QueryCommand.class,
    GraphQLCommand.PersistedCommand.class,
    GraphQLCommand.ListCommand.class
})
public class GraphQLCommand implements Callable<Integer> {

    /**
     * Shows usage information when called without subcommand.
     *
     * @return exit code 0
     */
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'graphql query', 'graphql persisted', or 'graphql list' for operations");
        return 0;
    }

    /**
     * Executes a GraphQL query.
     */
    @Command(name = "query", description = "Execute a GraphQL query")
    public static class QueryCommand implements Callable<Integer> {
        @Option(names = {"-q", "--query"}, description = "GraphQL query string", required = true)
        private String query;

        @Option(names = {"-v", "--variables"}, description = "JSON variables")
        private String variables;

        /**
         * Executes the GraphQL query.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if query execution fails
         */
        @Override
        public Integer call() throws Exception {
            ConfigManager config = ConfigManager.getInstance();
            String baseUrl = config.getActiveEnvironmentUrl();

            if (baseUrl == null || baseUrl.isEmpty()) {
                System.out.println("Not connected. Run 'connect' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();

                ObjectNode request = client.getObjectMapper().createObjectNode();
                request.put("query", query);
                if (variables != null) {
                    request.set("variables", client.getObjectMapper().readTree(variables));
                }

                JsonNode response = client.post("/graphql/execute.json", request);

                System.out.println("\nGraphQL Response:");
                System.out.println(response.toString());
                return 0;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Executes a persisted GraphQL query.
     */
    @Command(name = "persisted", description = "Execute a persisted GraphQL query")
    public static class PersistedCommand implements Callable<Integer> {
        @Option(names = {"-n", "--name"}, description = "Persisted query name", required = true)
        private String name;

        @Option(names = {"-v", "--variables"}, description = "JSON variables")
        private String variables;

        /**
         * Executes the persisted GraphQL query.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if query execution fails
         */
        @Override
        public Integer call() throws Exception {
            ConfigManager config = ConfigManager.getInstance();
            String baseUrl = config.getActiveEnvironmentUrl();

            if (baseUrl == null || baseUrl.isEmpty()) {
                System.out.println("Not connected. Run 'connect' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();

                String endpoint = "/graphql/execute.json/" + name;
                ObjectNode request = client.getObjectMapper().createObjectNode();
                if (variables != null) {
                    request.set("variables", client.getObjectMapper().readTree(variables));
                }

                JsonNode response = client.post(endpoint, request);

                System.out.println("\nPersisted Query Response:");
                System.out.println(response.toString());
                return 0;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Lists persisted GraphQL queries.
     */
    @Command(name = "list", description = "List persisted GraphQL queries")
    public static class ListCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Folder path", defaultValue = "/graphql/persisted-query")
        private String path;

        /**
         * Lists available persisted queries.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if listing fails
         */
        @Override
        public Integer call() throws Exception {
            ConfigManager config = ConfigManager.getInstance();
            String baseUrl = config.getActiveEnvironmentUrl();

            if (baseUrl == null || baseUrl.isEmpty()) {
                System.out.println("Not connected. Run 'connect' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();
                JsonNode response = client.get(path + ".1.json");

                System.out.println("\nPersisted Queries:");
                if (response.has("hits")) {
                    response.get("hits").forEach(hit -> {
                        System.out.println("  - " + hit.path("name").asText());
                    });
                } else {
                    System.out.println("  (none found)");
                }
                return 0;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
}
