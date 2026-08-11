package com.aemtools.aem.recipes;

import com.aemtools.aem.api.AssetsApi;
import com.aemtools.aem.api.PackagesApi;
import com.aemtools.aem.api.PackagesApi.Package;
import com.aemtools.aem.api.PagesApi;
import com.aemtools.aem.api.ReplicationApi;
import com.aemtools.aem.api.TagsApi;
import com.aemtools.aem.api.UsersApi;
import com.aemtools.aem.client.AemApiClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Shared engine for multi-step AEM recipes.
 * Used by the CLI ({@code recipe} command), the GUI, and the MCP server so that
 * automation runbooks have a single source of truth.
 */
public class RecipeEngine {

  public record RecipeResult(boolean success, List<String> steps, String error) {

    public static RecipeResult ok(List<String> steps) {
      return new RecipeResult(true, List.copyOf(steps), null);
    }

    public static RecipeResult fail(List<String> steps, String error) {
      return new RecipeResult(false, List.copyOf(steps), error);
    }
  }

  private final AemApiClient injectedClient;

  public RecipeEngine() {
    this(null);
  }

  /**
   * @param client an optional pre-configured client (e.g. pointed at an AEM as a
   *     Cloud Service author with an IMS token); when {@code null} each recipe
   *     builds a client for the globally active environment
   */
  public RecipeEngine(AemApiClient client) {
    this.injectedClient = client;
  }

  private AemApiClient client() {
    return injectedClient != null ? injectedClient : new AemApiClient();
  }

  /** Launch a new site: create root + sub-pages, optionally publish. */
  public RecipeResult siteLaunch(String sitePath, String title, String template, boolean publish) {
    List<String> steps = new ArrayList<>();
    steps.add("=== Site Launch Recipe ===");
    steps.add("Site: " + sitePath);
    steps.add("Title: " + (title != null ? title : sitePath));
    steps.add("Template: " + template);
    steps.add("Publish: " + publish);

    try {
      AemApiClient client = client();
      PagesApi pagesApi = new PagesApi(client);
      ReplicationApi replicationApi = new ReplicationApi(client);

      String parentPath = sitePath.substring(0, sitePath.lastIndexOf("/"));
      String name = sitePath.substring(sitePath.lastIndexOf("/") + 1);

      steps.add("Step 1: Creating site root...");
      pagesApi.create(parentPath, name, template, title != null ? title : name);

      steps.add("Step 2: Creating sub-pages...");
      pagesApi.create(sitePath, "home", template, "Home");
      pagesApi.create(sitePath, "about", template, "About Us");
      pagesApi.create(sitePath, "contact", template, "Contact");

      if (publish) {
        steps.add("Step 3: Publishing site structure...");
        replicationApi.publish(sitePath, null);
      }

      steps.add("Site launch recipe completed successfully!");
      return RecipeResult.ok(steps);
    } catch (Exception e) {
      steps.add("Site launch failed: " + e.getMessage());
      return RecipeResult.fail(steps, e.getMessage());
    }
  }

  /** Backup a content path by creating, building, and downloading a package. */
  public RecipeResult contentBackup(String path, String outputDir, String group) {
    List<String> steps = new ArrayList<>();
    steps.add("=== Content Backup Recipe ===");
    steps.add("Path: " + path);
    steps.add("Output Folder: " + outputDir);

    try {
      AemApiClient client = client();
      PackagesApi packagesApi = new PackagesApi(client);

      String timestamp = String.valueOf(System.currentTimeMillis());
      String backupName = "backup_" + path.replace("/", "_").substring(1) + "_" + timestamp;

      Path outPath = Paths.get(outputDir);
      if (!Files.exists(outPath)) {
        Files.createDirectories(outPath);
      }

      steps.add("Step 1: Creating backup package definition...");
      String filterXml = String.format(
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
              + "<workspaceFilter version=\"1.0\"><filter root=\"%s\"/></workspaceFilter>", path);
      PackagesApi.Package definition = packagesApi.uploadDefinition(filterXml);
      String serverGroup = definition.getGroup();
      String serverName = definition.getName();

      steps.add("Step 2: Building package...");
      boolean buildSuccess = packagesApi.build(serverGroup, serverName);
      if (!buildSuccess) {
        steps.add("Error: Package build failed on server.");
        return RecipeResult.fail(steps, "Package build failed on server.");
      }

      steps.add("Step 3: Downloading package...");
      Path localZip = outPath.resolve(backupName + ".zip");
      packagesApi.download(serverGroup, serverName, localZip);

      steps.add("Backup complete! File saved to: " + localZip.toAbsolutePath());
      return RecipeResult.ok(steps);
    } catch (Exception e) {
      steps.add("Backup failed: " + e.getMessage());
      return RecipeResult.fail(steps, e.getMessage());
    }
  }

  /** Batch process assets: upload, tag, and optionally publish each file. */
  public RecipeResult assetBatch(String localPath, String destPath, String tags, boolean publish) {
    List<String> steps = new ArrayList<>();
    steps.add("=== Asset Batch Recipe ===");
    steps.add("Local Path: " + localPath);
    steps.add("AEM Path: " + destPath);
    steps.add("Tags: " + (tags != null ? tags : "none"));
    steps.add("Publish: " + publish);

    try {
      AemApiClient client = client();
      AssetsApi assetsApi = new AssetsApi(client);
      TagsApi tagsApi = new TagsApi(client);
      ReplicationApi replicationApi = new ReplicationApi(client);

      Path folder = Paths.get(localPath);
      if (!Files.isDirectory(folder)) {
        steps.add("Error: " + localPath + " is not a directory");
        return RecipeResult.fail(steps, localPath + " is not a directory");
      }

      List<Path> files;
      try (Stream<Path> stream = Files.list(folder)) {
        files = stream.filter(Files::isRegularFile).collect(Collectors.toList());
      }

      steps.add("Found " + files.size() + " assets to process.");

      for (int i = 0; i < files.size(); i++) {
        Path file = files.get(i);
        String fileName = file.getFileName().toString();
        steps.add(String.format("  [%d/%d] Processing %s...", i + 1, files.size(), fileName));

        assetsApi.uploadFile(destPath, file);
        String assetPath = destPath + "/" + fileName;

        if (tags != null) {
          List<String> tagList = Arrays.asList(tags.split(","));
          tagsApi.applyTags(assetPath, tagList, false);
        }

        if (publish) {
          replicationApi.publish(assetPath, null);
        }
      }

      steps.add("Asset batch complete!");
      return RecipeResult.ok(steps);
    } catch (Exception e) {
      steps.add("Asset batch failed: " + e.getMessage());
      return RecipeResult.fail(steps, e.getMessage());
    }
  }

  /** Onboard a new user: create account and add to groups. */
  public RecipeResult userOnboard(String userId, String password, String email, String groups) {
    List<String> steps = new ArrayList<>();
    steps.add("=== User Onboarding Recipe ===");
    steps.add("User: " + userId);
    steps.add("Groups: " + groups);

    try {
      AemApiClient client = client();
      UsersApi usersApi = new UsersApi(client);

      steps.add("Step 1: Creating user " + userId + "...");
      usersApi.createUser(userId, password, email, null, null);

      steps.add("Step 2: Adding to groups: " + groups + "...");
      List<String> groupList = Arrays.asList(groups.split(","));
      for (String groupId : groupList) {
        try {
          usersApi.addUserToGroup(userId, groupId.trim());
          steps.add("  Added to " + groupId.trim());
        } catch (Exception e) {
          steps.add("  Failed to add to " + groupId + ": " + e.getMessage());
        }
      }

      steps.add("User onboarding recipe completed successfully!");
      return RecipeResult.ok(steps);
    } catch (Exception e) {
      steps.add("User onboarding failed: " + e.getMessage());
      return RecipeResult.fail(steps, e.getMessage());
    }
  }

  /** Migrate a package between AEM environments (download, upload, optional install). */
  public RecipeResult packageMigrate(String name, String group, String targetUrl,
      String targetAuth, String targetToken, boolean install) {
    List<String> steps = new ArrayList<>();
    steps.add("=== Package Migration Recipe ===");
    steps.add("Package: " + group + ":" + name);
    steps.add("Target: " + targetUrl);

    if (targetUrl == null || targetUrl.isEmpty()) {
      steps.add("Error: --target-url is required");
      return RecipeResult.fail(steps, "--target-url is required");
    }
    if ((targetAuth == null || targetAuth.isEmpty())
        && (targetToken == null || targetToken.isEmpty())) {
      steps.add("Error: --target-auth or --target-token is required");
      return RecipeResult.fail(steps, "--target-auth or --target-token is required");
    }

    String authHeader = null;
    if (targetToken != null && !targetToken.isEmpty()) {
      authHeader = targetToken.startsWith("Bearer ") ? targetToken : "Bearer " + targetToken;
    } else {
      authHeader = targetAuth.startsWith("Basic ") ? targetAuth : "Basic " + targetAuth;
    }

    Path tempDir = null;
    try {
      tempDir = Files.createTempDirectory("aem-pkg-migrate-");
      Path pkgPath = tempDir.resolve(name + ".zip");

      steps.add("Step 1: Downloading package from current environment...");
      AemApiClient sourceClient = client();
      PackagesApi sourceApi = new PackagesApi(sourceClient);
      sourceApi.download(group, name, pkgPath);
      steps.add("  Downloaded to: " + pkgPath.toAbsolutePath());

      steps.add("Step 2: Uploading package to target [" + targetUrl + "]...");
      AemApiClient targetClient = new AemApiClient().withTarget(targetUrl, authHeader);
      PackagesApi targetApi = new PackagesApi(targetClient);
      Package uploaded = targetApi.upload(pkgPath);
      steps.add("  Uploaded: " + uploaded.getGroup() + "/" + uploaded.getName() + ".zip");

      if (install) {
        steps.add("Step 3: Installing package on target...");
        boolean installed = targetApi.install(uploaded.getGroup(), uploaded.getName());
        if (!installed) {
          steps.add("  Warning: server reported install failure for "
              + uploaded.getGroup() + ":" + uploaded.getName());
          return RecipeResult.fail(steps, "Install failure for "
              + uploaded.getGroup() + ":" + uploaded.getName());
        }
        steps.add("  Installed: " + uploaded.getGroup() + ":" + uploaded.getName());
      }

      steps.add("Package migration recipe completed successfully!");
      return RecipeResult.ok(steps);
    } catch (Exception e) {
      steps.add("Migration failed: " + e.getMessage());
      return RecipeResult.fail(steps, e.getMessage());
    } finally {
      if (tempDir != null) {
        try (Stream<Path> stream = Files.list(tempDir)) {
          stream.forEach(p -> {
            try {
              Files.deleteIfExists(p);
            } catch (Exception ignored) {
            }
          });
        } catch (Exception ignored) {
        }
        try {
          Files.deleteIfExists(tempDir);
        } catch (Exception ignored) {
        }
      }
    }
  }
}
