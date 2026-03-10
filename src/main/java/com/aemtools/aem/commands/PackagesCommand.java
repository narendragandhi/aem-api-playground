package com.aemtools.aem.commands;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.util.MockDataHelper;
import com.fasterxml.jackson.databind.JsonNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

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

            System.out.println("Listing packages" + (group != null ? " in group: " + group : ""));
            System.out.println("(Use --mock for demo data)");
            return 0;
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
            System.out.println("Building package: " + group + "/" + name);
            System.out.println("(Package build not fully implemented)");
            return 0;
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
            System.out.println("Installing package: " + group + "/" + name);
            System.out.println("(Package install not fully implemented)");
            return 0;
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
            System.out.println("Uploading package: " + filePath);
            System.out.println("Group: " + group);
            System.out.println("(Package upload not fully implemented)");
            return 0;
        }
    }
}
