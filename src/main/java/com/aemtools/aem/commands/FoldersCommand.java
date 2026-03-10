package com.aemtools.aem.commands;

import com.aemtools.aem.api.AssetsApi;
import com.aemtools.aem.client.AemApiClient;
import com.aemtools.aem.config.ConfigManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command for AEM folder operations.
 * Supports creating, deleting, and listing folders in the DAM.
 */
@Command(name = "folders", description = "Folder operations", subcommands = {
    FoldersCommand.CreateCommand.class,
    FoldersCommand.DeleteCommand.class,
    FoldersCommand.ListCommand.class
})
public class FoldersCommand implements Callable<Integer> {

    /**
     * Shows usage information when called without subcommand.
     *
     * @return exit code 0
     */
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'folders create', 'folders delete', or 'folders list' for operations");
        return 0;
    }

    /**
     * Creates a new folder in the DAM.
     */
    @Command(name = "create", description = "Create a folder")
    public static class CreateCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Parent path", required = true)
        private String parentPath;

        @Option(names = {"-n", "--name"}, description = "Folder name", required = true)
        private String name;

        @Option(names = {"-t", "--title"}, description = "Folder title")
        private String title;

        /**
         * Executes the create folder command.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if folder creation fails
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

                System.out.println("Creating folder: " + name);
                System.out.println("Parent: " + parentPath);
                System.out.println("Title: " + (title != null ? title : name));

                AssetsApi.Folder folder = api.createFolder(parentPath, name, title);

                System.out.println("\nFolder created!");
                System.out.println("  Name: " + folder.getName());
                System.out.println("  Title: " + folder.getTitle());
                return 0;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Deletes a folder from the DAM.
     */
    @Command(name = "delete", description = "Delete a folder")
    public static class DeleteCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Folder path", required = true)
        private String path;

        @Option(names = {"--force"}, description = "Force delete (with children)")
        private boolean force;

        /**
         * Executes the delete folder command.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if folder deletion fails
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

                System.out.println("Deleting folder: " + path);
                boolean result = api.deleteFolder(path);

                if (result) {
                    System.out.println("\nFolder deleted!");
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

    /**
     * Lists folders in a given path.
     */
    @Command(name = "list", description = "List folders")
    public static class ListCommand implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Parent path", defaultValue = "/content/dam")
        private String path;

        /**
         * Executes the list folders command.
         *
         * @return exit code (0 for success, 1 for failure)
         * @throws Exception if listing fails
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

                List<AssetsApi.Folder> folders = api.listFolders(path);

                System.out.println("\nFolders in " + path + ":\n");
                for (AssetsApi.Folder folder : folders) {
                    System.out.println("  " + folder.getName() + " - " + folder.getTitle());
                }
                System.out.println("\nTotal: " + folders.size());
                return 0;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return 1;
            }
        }
    }
}
