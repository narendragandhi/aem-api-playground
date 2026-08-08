package com.aemtools.aem.commands;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.api.PackagesApi;
import com.aemtools.aem.api.PackagesApi.Package;
import com.aemtools.aem.client.AemApiClient;
import com.aemtools.aem.config.ConfigManager;
import com.aemtools.aem.util.MockDataHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command for AEM package operations.
 * Supports listing, building, installing, and uploading packages.
 */
@Command(name = "packages", description = "Package operations", subcommands = {
    PackagesCommand.ListCommand.class,
    PackagesCommand.BuildCommand.class,
    PackagesCommand.InstallCommand.class,
    PackagesCommand.UploadCommand.class
})
public class PackagesCommand implements Callable<Integer> {

    /**
     * Shows usage information when called without subcommand.
     *
     * @return exit code 0
     */
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'packages list', 'packages build', 'packages install', or 'packages upload' for operations");
        return 0;
    }

    /**
     * Lists packages in the package manager.
     */
    @Command(name = "list", description = "List packages")
    public static class ListCommand implements Callable<Integer> {
        @Option(names = {"-g", "--group"}, description = "Package group")
        private String group;

        /**
         * Executes the list packages command.
         *
         * @return exit code 0
         * @throws Exception if listing fails
         */
        @Override
        public Integer call() throws Exception {
            if (CliFlags.mockMode) {
                JsonNode mockData = MockDataHelper.getPackages();
                if (CliFlags.jsonOutput) {
                    System.out.println(mockData.toString());
                } else {
                    System.out.println("\n[MOCK MODE] Packages"
                        + (group != null ? " in group: " + group : " all groups") + ":\n");
                    for (JsonNode pkg : mockData) {
                        System.out.println("  " + pkg.get("group").asText() + ":"
                            + pkg.get("name").asText() + " v" + pkg.get("version").asText());
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
                PackagesApi packagesApi = new PackagesApi(new AemApiClient());
                List<Package> packages = packagesApi.list(group);

                if (CliFlags.jsonOutput) {
                    ObjectMapper mapper = new ObjectMapper();
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(packages));
                } else {
                    System.out.println("Listing packages" + (group != null ? " in group: " + group : "") + "\n");
                    if (packages.isEmpty()) {
                        System.out.println("  No packages found.");
                    } else {
                        for (Package pkg : packages) {
                            System.out.printf("  %s/%s v%s - %s%n",
                                pkg.getGroup(), pkg.getName(), pkg.getVersion(), pkg.getDescription());
                        }
                    }
                    System.out.println("\nTotal: " + packages.size());
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error listing packages: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Builds a package.
     */
    @Command(name = "build", description = "Build a package")
    public static class BuildCommand implements Callable<Integer> {
        @Option(names = {"-n", "--name"}, description = "Package name", required = true)
        private String name;

        @Option(names = {"-g", "--group"}, description = "Package group", defaultValue = "my_packages")
        private String group;

        /**
         * Executes the build package command.
         *
         * @return exit code 0
         * @throws Exception if build fails
         */
        @Override
        public Integer call() throws Exception {
            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                PackagesApi packagesApi = new PackagesApi(new AemApiClient());
                boolean success = packagesApi.build(group, name);
                if (success) {
                    System.out.println("Built package: " + group + "/" + name);
                    return 0;
                }
                System.err.println("Build failed: " + group + "/" + name);
                return 1;
            } catch (Exception e) {
                System.err.println("Error building package: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Installs a package.
     */
    @Command(name = "install", description = "Install a package")
    public static class InstallCommand implements Callable<Integer> {
        @Option(names = {"-n", "--name"}, description = "Package name", required = true)
        private String name;

        @Option(names = {"-g", "--group"}, description = "Package group", defaultValue = "my_packages")
        private String group;

        /**
         * Executes the install package command.
         *
         * @return exit code 0
         * @throws Exception if installation fails
         */
        @Override
        public Integer call() throws Exception {
            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                PackagesApi packagesApi = new PackagesApi(new AemApiClient());
                boolean success = packagesApi.install(group, name);
                if (success) {
                    System.out.println("Installed package: " + group + "/" + name);
                    return 0;
                }
                System.err.println("Install failed: " + group + "/" + name);
                return 1;
            } catch (Exception e) {
                System.err.println("Error installing package: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Uploads a package file.
     */
    @Command(name = "upload", description = "Upload a package")
    public static class UploadCommand implements Callable<Integer> {
        @Option(names = {"-f", "--file"}, description = "Package file path", required = true)
        private String filePath;

        @Option(names = {"-g", "--group"}, description = "Package group", defaultValue = "my_packages")
        private String group;

        /**
         * Executes the upload package command.
         *
         * @return exit code 0
         * @throws Exception if upload fails
         */
        @Override
        public Integer call() throws Exception {
            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                return 1;
            }

            try {
                PackagesApi packagesApi = new PackagesApi(new AemApiClient());
                Package uploaded = packagesApi.upload(java.nio.file.Path.of(filePath));
                System.out.println("Uploaded package: " + uploaded.getGroup() + "/" + uploaded.getName()
                    + " v" + uploaded.getVersion());
                return 0;
            } catch (Exception e) {
                System.err.println("Error uploading package: " + e.getMessage());
                return 1;
            }
        }
    }
}
