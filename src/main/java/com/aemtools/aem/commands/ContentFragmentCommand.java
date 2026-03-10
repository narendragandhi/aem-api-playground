package com.aemtools.aem.commands;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.api.ContentFragmentApi;
import com.aemtools.aem.client.AemApiClient;
import com.aemtools.aem.config.ConfigManager;
import com.aemtools.aem.util.MockDataHelper;
import com.fasterxml.jackson.databind.JsonNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command for Content Fragment operations.
 * Supports listing, getting, creating, exporting, and importing content fragments.
 */
@Command(name = "cf", description = "Content Fragment operations", subcommands = {
    ContentFragmentCommand.ListCommand.class,
    ContentFragmentCommand.GetCommand.class,
    ContentFragmentCommand.CreateCommand.class,
    ContentFragmentCommand.ExportCommand.class,
    ContentFragmentCommand.ImportCommand.class
})
public class ContentFragmentCommand implements Callable<Integer> {

    /**
     * Shows usage information when called without subcommand.
     *
     * @return exit code 0
     */
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'cf list', 'cf get', or 'cf create' for operations");
        return 0;
    }

    /**
     * Lists content fragments at a given path.
     */
    @Command(name = "list", description = "List content fragments")
    public static class ListCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Path to content fragments", defaultValue = "/content/dam")
        private String path;

        @Option(names = {"-m", "--max"}, description = "Max results", defaultValue = "20")
        private int max;

        @Option(names = {"--format"}, description = "Output format: table, json, raw", defaultValue = "table")
        private String format;

        /**
         * Executes the list command.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if API call fails
         */
        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode) {
                JsonNode mockData = MockDataHelper.getContentFragments();
                if (CliFlags.jsonOutput) {
                    System.out.println(mockData.toString());
                } else {
                    System.out.println("\n[MOCK MODE] Content Fragments in " + path + ":\n");
                    for (JsonNode cf : mockData) {
                        System.out.println("  " + cf.get("name").asText() + " - " + cf.get("title").asText());
                    }
                    System.out.println("\nTotal: " + mockData.size());
                }
                return 0;
            }

            ConfigManager config = ConfigManager.getInstance();
            String baseUrl = config.getActiveEnvironmentUrl();

            if (baseUrl == null || baseUrl.isEmpty()) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                AemApiClient client = new AemApiClient();
                ContentFragmentApi api = new ContentFragmentApi(client);

                List<ContentFragmentApi.ContentFragment> fragments = api.list(path, max);

                System.out.println("\nContent Fragments in " + path + ":\n");
                for (ContentFragmentApi.ContentFragment cf : fragments) {
                    System.out.println("  " + cf.getName() + " - " + cf.getTitle());
                }
                System.out.println("\nTotal: " + fragments.size());
                return 0;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Gets a specific content fragment by path.
     */
    @Command(name = "get", description = "Get a content fragment by path")
    public static class GetCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Content fragment path", required = true)
        private String path;

        /**
         * Executes the get command.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if API call fails
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
                ContentFragmentApi api = new ContentFragmentApi(client);

                ContentFragmentApi.ContentFragment cf = api.get(path);

                System.out.println("\nContent Fragment:");
                System.out.println("  Name: " + cf.getName());
                System.out.println("  Title: " + cf.getTitle());
                System.out.println("  Model: " + cf.getModel());
                System.out.println("  Path: " + cf.getPath());
                System.out.println("  Description: " + cf.getDescription());
                return 0;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Creates a new content fragment.
     */
    @Command(name = "create", description = "Create a new content fragment")
    public static class CreateCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Parent path", required = true)
        private String parentPath;

        @Option(names = {"-n", "--name"}, description = "Fragment name", required = true)
        private String name;

        @Option(names = {"-m", "--model"}, description = "Content fragment model path", required = true)
        private String model;

        @Option(names = {"-t", "--title"}, description = "Fragment title")
        private String title;

        /**
         * Executes the create command.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if API call fails
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
                ContentFragmentApi api = new ContentFragmentApi(client);

                ContentFragmentApi.ContentFragment cf = api.create(parentPath, name, model, title);

                System.out.println("\nContent Fragment created!");
                System.out.println("  Name: " + cf.getName());
                System.out.println("  Title: " + cf.getTitle());
                return 0;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Exports content fragments to JSON format.
     */
    @Command(name = "export", description = "Export content fragments to JSON")
    public static class ExportCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Path to content fragments", defaultValue = "/content/dam")
        private String path;

        @Option(names = {"-m", "--max"}, description = "Max results", defaultValue = "50")
        private int max;

        @Option(names = {"-o", "--output"}, description = "Output file path")
        private String outputFile;

        /**
         * Executes the export command.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if API call or file write fails
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
                ContentFragmentApi api = new ContentFragmentApi(client);

                String json = api.exportToJson(path, max);

                if (outputFile != null) {
                    java.nio.file.Files.writeString(java.nio.file.Paths.get(outputFile), json);
                    System.out.println("Exported to " + outputFile);
                } else {
                    System.out.println(json);
                }
                return 0;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Imports content fragments from JSON format.
     */
    @Command(name = "import", description = "Import content fragments from JSON")
    public static class ImportCommand implements Callable<Integer> {
        @Option(names = {"-f", "--file"}, description = "Input JSON file (or - for stdin)")
        private String inputFile;

        @Option(names = {"-t", "--target"}, description = "Target parent path", defaultValue = "/content/dam")
        private String targetPath;

        @Option(names = {"-d", "--data"}, description = "JSON data directly")
        private String jsonData;

        /**
         * Executes the import command.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if API call or file read fails
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
                String jsonInput;
                if (inputFile != null && !inputFile.equals("-")) {
                    jsonInput = java.nio.file.Files.readString(java.nio.file.Paths.get(inputFile));
                } else if (jsonData != null) {
                    jsonInput = jsonData;
                } else {
                    System.out.println("Error: Specify --file or --data");
                    return 1;
                }

                AemApiClient client = new AemApiClient();
                ContentFragmentApi api = new ContentFragmentApi(client);

                int count = api.importFromJson(jsonInput, targetPath);
                System.out.println("Imported " + count + " content fragments to " + targetPath);
                return 0;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
}
