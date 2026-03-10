package com.aemtools.aem.commands;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.api.AssetsApi;
import com.aemtools.aem.client.AemApiClient;
import com.aemtools.aem.config.ConfigManager;
import com.aemtools.aem.util.MockDataHelper;
import com.fasterxml.jackson.databind.JsonNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command for AEM Assets operations.
 * Supports listing, uploading, and deleting digital assets.
 */
@Command(name = "assets", description = "Assets operations", subcommands = {
    AssetsCommand.ListCommand.class,
    AssetsCommand.UploadCommand.class,
    AssetsCommand.DeleteCommand.class
})
public class AssetsCommand implements Callable<Integer> {

    /**
     * Shows usage information when called without subcommand.
     *
     * @return exit code 0
     */
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'assets list', 'assets upload', or 'assets delete' for operations");
        return 0;
    }

    /**
     * Lists assets at a given path.
     */
    @Command(name = "list", description = "List assets")
    public static class ListCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Folder path", defaultValue = "/content/dam")
        private String path;

        @Option(names = {"-m", "--max"}, description = "Max results", defaultValue = "20")
        private int max;

        /**
         * Executes the list command.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if API call fails
         */
        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode) {
                JsonNode mockData = MockDataHelper.getAssets();
                if (CliFlags.jsonOutput) {
                    System.out.println(mockData.toString());
                } else {
                    System.out.println("\n[MOCK MODE] Assets in " + path + ":\n");
                    for (JsonNode asset : mockData) {
                        System.out.println("  " + asset.get("name").asText() + " - " + asset.get("title").asText());
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
                AssetsApi api = new AssetsApi(client);

                List<AssetsApi.Asset> assets = api.list(path, max);

                System.out.println("\nAssets in " + path + ":\n");
                if (assets.isEmpty()) {
                    System.out.println("  (no assets found - trying subfolders)");
                    List<AssetsApi.Folder> folders = api.listFolders(path);
                    for (AssetsApi.Folder folder : folders) {
                        System.out.println("  [folder] " + folder.getName());
                    }
                } else {
                    for (AssetsApi.Asset asset : assets) {
                        System.out.println("  " + asset);
                    }
                }
                System.out.println("\nTotal: " + assets.size());
                return 0;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                e.printStackTrace();
                return 1;
            }
        }
    }

    /**
     * Uploads an asset to AEM DAM.
     */
    @Command(name = "upload", description = "Upload an asset")
    public static class UploadCommand implements Callable<Integer> {
        @Option(names = {"-f", "--file"}, description = "File to upload", required = true)
        private String filePath;

        @Option(names = {"-p", "--path"}, description = "Destination folder", defaultValue = "/content/dam")
        private String folderPath;

        /**
         * Executes the upload command.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if upload fails
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
                Path path = Paths.get(filePath);
                if (!Files.exists(path)) {
                    System.out.println("File not found: " + filePath);
                    return 1;
                }

                AemApiClient client = new AemApiClient();
                AssetsApi api = new AssetsApi(client);

                System.out.println("Uploading: " + filePath + " to " + folderPath);
                AssetsApi.Asset asset = api.uploadFile(folderPath, path);

                System.out.println("\nUpload successful!");
                System.out.println("  Path: " + asset.getPath());
                System.out.println("  Name: " + asset.getName());
                return 0;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Deletes an asset from AEM DAM.
     */
    @Command(name = "delete", description = "Delete an asset")
    public static class DeleteCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Asset path", required = true)
        private String path;

        /**
         * Executes the delete command.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if deletion fails
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
                AssetsApi api = new AssetsApi(client);

                System.out.println("Deleting: " + path);
                boolean result = api.delete(path);

                if (result) {
                    System.out.println("\nDelete successful!");
                } else {
                    System.out.println("\nDelete failed!");
                }
                return result ? 0 : 1;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
}
