package com.aemtools.aem.commands;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.api.AssetsApi;
import com.aemtools.aem.api.ReplicationApi;
import com.aemtools.aem.api.TagsApi;
import com.aemtools.aem.api.UsersApi;
import com.aemtools.aem.client.AemApiClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command for executing predefined multi-step recipes.
 * Recipes are complex workflows that combine multiple AEM operations
 * into a single, reusable sequence.
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
        @Option(names = {"-p", "--path"}, description = "Site root path", required = true)
        private String sitePath;

        @Option(names = {"-t", "--title"}, description = "Site title")
        private String title;

        @Option(names = {"--publish"},
                description = "Publish after creation",
                defaultValue = "true")
        private boolean publish;

        @Override
        public Integer call() throws Exception {
            System.out.println("\n=== Site Launch Recipe ===");
            System.out.println("Site: " + sitePath);
            System.out.println("Title: " + (title != null ? title : sitePath));
            System.out.println("Publish: " + publish);

            if (CliFlags.mockMode || CliFlags.dryRunMode) {
                String mode = CliFlags.mockMode ? "MOCK MODE" : "DRY RUN";
                System.out.println("\n[" + mode + "] Would execute:");
                System.out.println("  1. Create site structure at " + sitePath);
                System.out.println("  2. Create content pages (home, about, contact)");
                System.out.println("  3. Add sample content fragments");
                System.out.println("  4. Upload sample assets");
                if (publish) {
                    System.out.println("  5. Publish site to author/publish instances");
                    System.out.println("  6. Clear dispatcher cache");
                }
                return 0;
            }

            System.out.println("\nExecuting steps...");
            System.out.println("  [1/5] Creating site structure... (Not fully implemented yet)");
            System.out.println("  [2/5] Creating content pages...");
            System.out.println("  [3/5] Adding sample content...");
            System.out.println("  [4/5] Uploading assets...");
            if (publish) {
                System.out.println("  [5/5] Publishing site...");
            }
            System.out.println("\nSite launch recipe completed (partially implemented)!");
            return 0;
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

            AemApiClient client = new AemApiClient();
            PackagesApi packagesApi = new PackagesApi(client);

            String timestamp = String.valueOf(System.currentTimeMillis());
            String packageName = "backup_" + path.replace("/", "_").substring(1) + "_" + timestamp;

            try {
                // Ensure output directory exists
                Path outPath = Paths.get(outputDir);
                if (!Files.exists(outPath)) {
                    Files.createDirectories(outPath);
                }

                System.out.println("\nStep 1: Creating backup package definition...");
                // Note: Recreate uses a filter XML. Minimal one for the path:
                String filterXml = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<workspaceFilter version=\"1.0\"><filter root=\"%s\"/></workspaceFilter>", path);
                packagesApi.recreate(group, packageName, filterXml);

                System.out.println("Step 2: Building package...");
                boolean buildSuccess = packagesApi.build(group, packageName);
                if (!buildSuccess) {
                    System.err.println("Error: Package build failed on server.");
                    return 1;
                }

                System.out.println("Step 3: Downloading package...");
                Path localZip = outPath.resolve(packageName + ".zip");
                packagesApi.download(group, packageName, localZip);

                System.out.println("\nBackup complete! File saved to: " + localZip.toAbsolutePath());
                
                // Cleanup? Usually good practice to delete the temporary package on the server
                // System.out.println("Step 4: Cleaning up temporary package on server...");
                // packagesApi.delete(group, packageName);

            } catch (Exception e) {
                System.err.println("\nBackup failed: " + e.getMessage());
                return 1;
            }

            return 0;
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

            AemApiClient client = new AemApiClient();
            AssetsApi assetsApi = new AssetsApi(client);
            TagsApi tagsApi = new TagsApi(client);
            ReplicationApi replicationApi = new ReplicationApi(client);

            Path folder = Paths.get(localPath);
            if (!Files.isDirectory(folder)) {
                System.err.println("Error: " + localPath + " is not a directory");
                return 1;
            }

            List<Path> files;
            try (Stream<Path> stream = Files.list(folder)) {
                files = stream.filter(Files::isRegularFile).collect(Collectors.toList());
            }

            System.out.println("\nFound " + files.size() + " assets to process.");

            for (int i = 0; i < files.size(); i++) {
                Path file = files.get(i);
                String fileName = file.getFileName().toString();
                System.out.println(String.format("  [%d/%d] Processing %s...", i + 1, files.size(), fileName));

                // 1. Upload
                AssetsApi.Asset asset = assetsApi.uploadFile(destPath, file);
                String assetPath = destPath + "/" + fileName;

                // 2. Tag
                if (tags != null) {
                    List<String> tagList = Arrays.asList(tags.split(","));
                    tagsApi.applyTags(assetPath, tagList, false);
                }

                // 3. Publish
                if (publish) {
                    replicationApi.publish(assetPath, null);
                }
            }

            System.out.println("\nAsset batch complete!");
            return 0;
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

            AemApiClient client = new AemApiClient();
            UsersApi usersApi = new UsersApi(client);

            System.out.println("\nStep 1: Creating user " + userId + "...");
            usersApi.createUser(userId, password, email, null, null);

            System.out.println("Step 2: Adding to groups: " + groups + "...");
            List<String> groupList = Arrays.asList(groups.split(","));
            for (String groupId : groupList) {
                try {
                    usersApi.addUserToGroup(userId, groupId.trim());
                    System.out.println("  Added to " + groupId.trim());
                } catch (Exception e) {
                    System.err.println("  Failed to add to " + groupId + ": " + e.getMessage());
                }
            }

            System.out.println("\nUser onboarding recipe completed successfully!");
            return 0;
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

        @Override
        public Integer call() throws Exception {
            System.out.println("\n=== Package Migration Recipe ===");
            if (CliFlags.mockMode || CliFlags.dryRunMode) {
                System.out.println("Simulating package migration for " + name);
                return 0;
            }
            System.out.println("Migration complete (placeholder implementation)!");
            return 0;
        }
    }
}
