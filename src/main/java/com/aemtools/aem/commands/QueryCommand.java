package com.aemtools.aem.commands;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.api.QueryApi;
import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command for generic AEM content search via the Query Builder servlet —
 * Adobe's documented content-search API ({@code /bin/querybuilder.json}).
 */
@Command(name = "query", description = "Run Query Builder searches against AEM", subcommands = {
    QueryCommand.RunCommand.class
})
public class QueryCommand implements Callable<Integer> {

    private static AemApiClient sharedClient;

    public static void setSharedClient(AemApiClient client) {
        sharedClient = client;
    }

    private static QueryApi getApi() {
        if (sharedClient == null) {
            sharedClient = new AemApiClient();
        }
        return new QueryApi(sharedClient);
    }

    @Override
    public Integer call() {
        System.out.println("Query Builder commands:");
        System.out.println("  query run - Run a Query Builder search");
        return 0;
    }

    @Command(name = "run", description = "Run a Query Builder search")
    public static class RunCommand implements Callable<Integer> {

        @Option(names = {"-p", "--path"}, description = "Content path to search under (default /content)")
        private String path;

        @Option(names = {"-t", "--type"}, description = "Primary node type filter (e.g. cq:Page, dam:Asset)")
        private String type;

        @Option(names = {"-q", "--param"}, description = "Extra Query Builder parameters as key=value "
            + "(repeatable), e.g. --param property=cq:tags --param property.value=geo:us")
        private List<String> params = new ArrayList<>();

        @Option(names = {"-m", "--limit"}, description = "Maximum hits (0 = unbounded with guessTotal)", defaultValue = "20")
        private int limit;

        @Option(names = {"--full"}, description = "Print the full Query Builder response")
        private boolean full;

        @Override
        public Integer call() throws Exception {
            String queryPath = QueryApi.buildQueryPath(path, type, params, limit);

            if (CliFlags.dryRunMode || CliFlags.mockMode) {
                String mode = CliFlags.mockMode ? "MOCK MODE" : "dry-run";
                System.out.println("[" + mode + "] Query URL: " + queryPath);
                return 0;
            }

            try {
                QueryApi api = getApi();
                JsonNode response = api.search(path, type, params, limit);

                if (CliFlags.jsonOutput || full) {
                    System.out.println(response.toPrettyString());
                    return 0;
                }

                List<String> paths = QueryApi.hitPaths(response);
                System.out.println("\n=== Query results (" + paths.size() + " shown, total "
                    + response.path("total").asInt(paths.size()) + ") ===");
                if (!paths.isEmpty()) {
                    System.out.println("-".repeat(60));
                }
                for (String hit : paths) {
                    System.out.println(hit);
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error running query: " + e.getMessage());
                return 1;
            }
        }
    }
}
