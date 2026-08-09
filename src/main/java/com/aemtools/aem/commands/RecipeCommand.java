package com.aemtools.aem.commands;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.recipes.RecipeEngine;
import com.aemtools.aem.recipes.RecipeEngine.RecipeResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Command for executing predefined multi-step recipes.
 * Recipes are complex workflows that combine multiple AEM operations
 * into a single, reusable sequence. The execution logic lives in
 * {@link RecipeEngine} so the CLI, GUI, and MCP server share one implementation.
 */
@Command(name = "recipe", description = "Execute predefined multi-step recipes", subcommands = {
    RecipeCommand.SiteLaunchRecipe.class,
    RecipeCommand.ContentBackupRecipe.class,
    RecipeCommand.AssetBatchRecipe.class,
    RecipeCommand.UserOnboardingRecipe.class,
    RecipeCommand.PackageMigrateRecipe.class
})
public class RecipeCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("Available recipes:");
        System.out.println("  site-launch      - Launch a new site (create pages, assets, publish)");
        System.out.println("  content-backup   - Backup content (export CFs, download packages)");
        System.out.println("  asset-batch      - Batch process assets (upload, tag, publish)");
        System.out.println("  user-onboard     - Onboard a new user (create, add to groups)");
        System.out.println("  package-migrate  - Migrate package between environments");
        System.out.println("\nUse: aem-api recipe <name> [options]");
        return 0;
    }

    /**
     * Recipe to launch a new site with pages, content, and assets.
     */
    @Command(name = "site-launch",
             description = "Launch a new site: create pages, add sample content, publish")
    public static class SiteLaunchRecipe implements Callable<Integer> {
        @Option(names = {"-p", "--path"}, description = "Site root path (e.g. /content/mysite)", required = true)
        private String sitePath;

        @Option(names = {"-t", "--title"}, description = "Site title")
        private String title;

        @Option(names = {"--template"}, description = "Page template path", defaultValue = "/conf/core-components-examples/settings/wcm/templates/content-page")
        private String template;

        @Option(names = {"--publish"},
                description = "Publish after creation",
                defaultValue = "true")
        private boolean publish;

        @Override
        public Integer call() throws Exception {
            System.out.println("\n=== Site Launch Recipe ===");
            System.out.println("Site: " + sitePath);
            System.out.println("Title: " + (title != null ? title : sitePath));
            System.out.println("Template: " + template);
            System.out.println("Publish: " + publish);

            if (CliFlags.mockMode || CliFlags.dryRunMode) {
                String mode = CliFlags.mockMode ? "MOCK MODE" : "DRY RUN";
                System.out.println("\n[" + mode + "] Would create site at " + sitePath + " and subpages.");
                return 0;
            }

            RecipeResult result = new RecipeEngine().siteLaunch(sitePath, title, template, publish);
            printResult(result);
            return result.success() ? 0 : 1;
        }
    }

    /**
     * Recipe to backup content including content fragments and packages.
     */
    @Command(name = "content-backup",
             description = "Backup content: create package for path, build and download")
    public static class ContentBackupRecipe implements Callable<Integer> {
        @Option(names = {"-p", "--path"},
                description = "Content path to backup (e.g. /content/dam/myapp)",
                required = true)
        private String path;

        @Option(names = {"-o", "--output"},
                description = "Output directory for the .zip package",
                defaultValue = "./backup")
        private String outputDir;

        @Option(names = {"--group"},
                description = "Package group name",
                defaultValue = "backups")
        private String group;

        @Override
        public Integer call() throws Exception {
            System.out.println("\n=== Content Backup Recipe ===");
            System.out.println("Path: " + path);
            System.out.println("Output Folder: " + outputDir);

            if (CliFlags.mockMode || CliFlags.dryRunMode) {
                String mode = CliFlags.mockMode ? "MOCK MODE" : "DRY RUN";
                System.out.println("\n[" + mode + "] Would create and download package for " + path);
                return 0;
            }

            RecipeResult result = new RecipeEngine().contentBackup(path, outputDir, group);
            printResult(result);
            return result.success() ? 0 : 1;
        }
    }

    /**
     * Recipe to batch process assets including upload, tagging, and publishing.
     */
    @Command(name = "asset-batch",
             description = "Batch process assets: upload, tag, publish")
    public static class AssetBatchRecipe implements Callable<Integer> {
        @Option(names = {"-p", "--path"},
                description = "Local folder path containing assets",
                required = true)
        private String localPath;

        @Option(names = {"-d", "--destination"},
                description = "AEM DAM destination path",
                required = true)
        private String destPath;

        @Option(names = {"-t", "--tags"}, description = "Tags to apply (comma-separated)")
        private String tags;

        @Option(names = {"--publish"},
                description = "Publish after processing",
                defaultValue = "true")
        private boolean publish;

        @Override
        public Integer call() throws Exception {
            System.out.println("\n=== Asset Batch Recipe ===");
            System.out.println("Local Path: " + localPath);
            System.out.println("AEM Path: " + destPath);
            System.out.println("Tags: " + (tags != null ? tags : "none"));
            System.out.println("Publish: " + publish);

            if (CliFlags.mockMode || CliFlags.dryRunMode) {
                String mode = CliFlags.mockMode ? "MOCK MODE" : "DRY RUN";
                System.out.println("\n[" + mode + "] Would execute batch asset processing.");
                return 0;
            }

            RecipeResult result = new RecipeEngine().assetBatch(localPath, destPath, tags, publish);
            printResult(result);
            return result.success() ? 0 : 1;
        }
    }

    /**
     * Recipe to onboard a new user with group assignments and permissions.
     */
    @Command(name = "user-onboard",
             description = "Onboard new user: create, add to groups, set permissions")
    public static class UserOnboardingRecipe implements Callable<Integer> {
        @Option(names = {"-u", "--user-id"}, description = "User ID", required = true)
        private String userId;

        @Option(names = {"-p", "--password"}, description = "User password", required = true)
        private String password;

        @Option(names = {"-e", "--email"}, description = "User email")
        private String email;

        @Option(names = {"-g", "--groups"},
                description = "Groups (comma-separated)",
                defaultValue = "contributors")
        private String groups;

        @Override
        public Integer call() throws Exception {
            System.out.println("\n=== User Onboarding Recipe ===");
            System.out.println("User: " + userId);
            System.out.println("Groups: " + groups);

            if (CliFlags.mockMode || CliFlags.dryRunMode) {
                String mode = CliFlags.mockMode ? "MOCK MODE" : "DRY RUN";
                System.out.println("\n[" + mode + "] Would onboard user " + userId);
                return 0;
            }

            RecipeResult result = new RecipeEngine().userOnboard(userId, password, email, groups);
            printResult(result);
            return result.success() ? 0 : 1;
        }
    }

    /**
     * Recipe to migrate packages between AEM environments.
     */
    @Command(name = "package-migrate",
             description = "Migrate package between environments")
    public static class PackageMigrateRecipe implements Callable<Integer> {
        @Option(names = {"-n", "--name"}, description = "Package name", required = true)
        private String name;

        @Option(names = {"-g", "--group"},
                description = "Package group",
                defaultValue = "my_packages")
        private String group;

        @Option(names = {"-t", "--target-url"},
                description = "Target AEM URL",
                required = true)
        private String targetUrl;

        @Option(names = {"--target-auth"},
                description = "Target Basic Auth (base64 encoded user:pass)")
        private String targetAuth;

        @Option(names = {"--target-token"},
                description = "Target Bearer access token")
        private String targetToken;

        @Option(names = {"--install"},
                description = "Install after upload",
                defaultValue = "true")
        private boolean install;

        @Override
        public Integer call() throws Exception {
            System.out.println("\n=== Package Migration Recipe ===");
            System.out.println("Package: " + group + ":" + name);
            System.out.println("Target: " + targetUrl);

            if (CliFlags.mockMode || CliFlags.dryRunMode) {
                String mode = CliFlags.mockMode ? "MOCK MODE" : "DRY RUN";
                System.out.println("\n[" + mode + "] Would migrate package " + name + " to " + targetUrl);
                return 0;
            }

            RecipeResult result = new RecipeEngine().packageMigrate(
                name, group, targetUrl, targetAuth, targetToken, install);
            printResult(result);
            return result.success() ? 0 : 1;
        }
    }

    private static void printResult(RecipeResult result) {
        result.steps().forEach(System.out::println);
        if (!result.success() && result.error() != null) {
            System.err.println("Error: " + result.error());
        }
    }
}
