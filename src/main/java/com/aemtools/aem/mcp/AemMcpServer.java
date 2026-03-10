package com.aemtools.aem.mcp;

import com.aemtools.aem.api.AssetsApi;
import com.aemtools.aem.api.ContentFragmentApi;
import com.aemtools.aem.api.GraphQLApi;
import com.aemtools.aem.api.PagesApi;
import com.aemtools.aem.api.PackagesApi;
import com.aemtools.aem.api.ReplicationApi;
import com.aemtools.aem.api.TagsApi;
import com.aemtools.aem.api.UsersApi;
import com.aemtools.aem.api.WorkflowApi;
import com.aemtools.aem.client.AemApiClient;
import com.aemtools.aem.config.ConfigManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * MCP Server for Adobe Experience Manager (AEM) operations.
 * Implements the Model Context Protocol over stdio (JSON-RPC 2.0).
 * Provides Claude Code with direct access to AEM APIs.
 */
public class AemMcpServer {

  private static final String MCP_VERSION = "2024-11-05";
  private static final String SERVER_NAME = "aem-mcp-server";
  private static final String SERVER_VERSION = "1.0.0";

  private final ObjectMapper mapper = new ObjectMapper();
  private final PrintWriter out;
  private final BufferedReader in;

  private AemApiClient client;
  private WorkflowApi workflowApi;
  private AssetsApi assetsApi;
  private ContentFragmentApi contentFragmentApi;
  private TagsApi tagsApi;
  private UsersApi usersApi;
  private ReplicationApi replicationApi;
  private GraphQLApi graphqlApi;
  private PagesApi pagesApi;
  private PackagesApi packagesApi;

  public AemMcpServer() {
    this.out = new PrintWriter(System.out, true);
    this.in = new BufferedReader(new InputStreamReader(System.in));
  }

  public static void main(String[] args) {
    AemMcpServer server = new AemMcpServer();
    server.run();
  }

  private void initializeApis() {
    if (client == null) {
      client = new AemApiClient();
      workflowApi = new WorkflowApi(client);
      assetsApi = new AssetsApi(client);
      contentFragmentApi = new ContentFragmentApi(client);
      tagsApi = new TagsApi(client);
      usersApi = new UsersApi(client);
      replicationApi = new ReplicationApi(client);
      graphqlApi = new GraphQLApi(client);
      pagesApi = new PagesApi(client);
      packagesApi = new PackagesApi(client);
    }
  }

  public void run() {
    try {
      String line;
      while ((line = in.readLine()) != null) {
        if (line.trim().isEmpty()) {
          continue;
        }
        try {
          JsonNode request = mapper.readTree(line);
          JsonNode response = handleRequest(request);
          out.println(mapper.writeValueAsString(response));
        } catch (Exception e) {
          ObjectNode error = createErrorResponse(null, -32700, "Parse error: " + e.getMessage());
          out.println(mapper.writeValueAsString(error));
        }
      }
    } catch (Exception e) {
      System.err.println("Server error: " + e.getMessage());
    }
  }

  private JsonNode handleRequest(JsonNode request) {
    String method = request.path("method").asText();
    JsonNode params = request.get("params");
    JsonNode id = request.get("id");

    try {
      Object result = switch (method) {
        case "initialize" -> handleInitialize(params);
        case "tools/list" -> handleToolsList();
        case "tools/call" -> handleToolsCall(params);
        case "ping" -> Map.of();
        default -> throw new IllegalArgumentException("Unknown method: " + method);
      };

      return createSuccessResponse(id, result);
    } catch (Exception e) {
      return createErrorResponse(id, -32603, e.getMessage());
    }
  }

  private Object handleInitialize(JsonNode params) {
    return Map.of(
        "protocolVersion", MCP_VERSION,
        "capabilities", Map.of(
            "tools", Map.of()
        ),
        "serverInfo", Map.of(
            "name", SERVER_NAME,
            "version", SERVER_VERSION
        )
    );
  }

  private Object handleToolsList() {
    ArrayNode tools = mapper.createArrayNode();

    // Connection tools
    tools.add(buildTool("aem_connect", "Connect to an AEM environment",
        Map.of(
            "environment", prop("string", "Environment name (dev, staging, prod)", true),
            "url", prop("string", "AEM server URL (optional)", false),
            "username", prop("string", "Username (optional)", false),
            "password", prop("string", "Password (optional)", false)
        )));

    tools.add(buildTool("aem_status", "Get current connection status",
        Map.of()));

    // Workflow tools
    tools.add(buildTool("aem_workflow_list", "List workflow instances",
        Map.of(
            "status", prop("string", "Filter: RUNNING, COMPLETED, ABORTED, SUSPENDED", false),
            "limit", prop("integer", "Max results (default 50)", false)
        )));

    tools.add(buildTool("aem_workflow_start", "Start a new workflow",
        Map.of(
            "model", prop("string", "Workflow model path", true),
            "payload", prop("string", "Content path to process", true)
        )));

    tools.add(buildTool("aem_workflow_terminate", "Terminate a workflow",
        Map.of("instanceId", prop("string", "Workflow instance ID", true))));

    tools.add(buildTool("aem_workflow_models", "List workflow models", Map.of()));

    tools.add(buildTool("aem_workflow_stats", "Get workflow statistics", Map.of()));

    // Asset tools
    tools.add(buildTool("aem_assets_list", "List assets in a folder",
        Map.of(
            "path", prop("string", "DAM folder path", true),
            "limit", prop("integer", "Max results (default 50)", false)
        )));

    tools.add(buildTool("aem_assets_get", "Get asset details",
        Map.of("path", prop("string", "Asset path", true))));

    tools.add(buildTool("aem_assets_delete", "Delete an asset",
        Map.of("path", prop("string", "Asset path", true))));

    tools.add(buildTool("aem_assets_move", "Move an asset",
        Map.of(
            "source", prop("string", "Source path", true),
            "destination", prop("string", "Destination path", true)
        )));

    tools.add(buildTool("aem_assets_search", "Search for assets",
        Map.of(
            "query", prop("string", "Search query", true),
            "limit", prop("integer", "Max results (default 20)", false)
        )));

    tools.add(buildTool("aem_folder_create", "Create a DAM folder",
        Map.of(
            "parentPath", prop("string", "Parent folder path", true),
            "name", prop("string", "Folder name", true),
            "title", prop("string", "Display title (optional)", false)
        )));

    // Content Fragment tools
    tools.add(buildTool("aem_cf_list", "List content fragments",
        Map.of(
            "path", prop("string", "Folder path", true),
            "limit", prop("integer", "Max results (default 50)", false)
        )));

    tools.add(buildTool("aem_cf_get", "Get content fragment details",
        Map.of("path", prop("string", "Fragment path", true))));

    tools.add(buildTool("aem_cf_create", "Create a content fragment",
        Map.of(
            "parentPath", prop("string", "Parent folder", true),
            "name", prop("string", "Fragment name", true),
            "model", prop("string", "Model path", true),
            "title", prop("string", "Display title (optional)", false)
        )));

    tools.add(buildTool("aem_cf_delete", "Delete a content fragment",
        Map.of("path", prop("string", "Fragment path", true))));

    tools.add(buildTool("aem_cf_export", "Export fragments to JSON",
        Map.of(
            "path", prop("string", "Folder path", true),
            "limit", prop("integer", "Max fragments (default 100)", false)
        )));

    // Tag tools
    tools.add(buildTool("aem_tags_list", "List tags",
        Map.of(
            "path", prop("string", "Tag path (default /content/cq:tags)", false),
            "recursive", prop("boolean", "Include nested tags", false),
            "limit", prop("integer", "Max results (default 100)", false)
        )));

    tools.add(buildTool("aem_tags_namespaces", "List tag namespaces", Map.of()));

    tools.add(buildTool("aem_tags_create", "Create a tag",
        Map.of(
            "tagId", prop("string", "Tag ID (namespace:tag)", true),
            "title", prop("string", "Display title", true),
            "description", prop("string", "Description (optional)", false)
        )));

    tools.add(buildTool("aem_tags_delete", "Delete a tag",
        Map.of(
            "tagId", prop("string", "Tag ID", true),
            "force", prop("boolean", "Force delete if in use", false)
        )));

    tools.add(buildTool("aem_tags_apply", "Apply tags to content",
        Map.of(
            "contentPath", prop("string", "Content path", true),
            "tags", prop("array", "Tag IDs to apply", true),
            "replace", prop("boolean", "Replace existing (default false)", false)
        )));

    tools.add(buildTool("aem_tags_usage", "Get tag usage count",
        Map.of("tagId", prop("string", "Tag ID", true))));

    // User tools
    tools.add(buildTool("aem_users_list", "List users",
        Map.of(
            "path", prop("string", "Users path (default /home/users)", false),
            "limit", prop("integer", "Max results (default 50)", false)
        )));

    tools.add(buildTool("aem_users_get", "Get user details with groups",
        Map.of("userId", prop("string", "User ID", true))));

    tools.add(buildTool("aem_users_create", "Create a user",
        Map.of(
            "userId", prop("string", "User ID", true),
            "password", prop("string", "Password", true),
            "email", prop("string", "Email (optional)", false),
            "givenName", prop("string", "First name (optional)", false),
            "familyName", prop("string", "Last name (optional)", false)
        )));

    tools.add(buildTool("aem_users_delete", "Delete a user",
        Map.of("userId", prop("string", "User ID", true))));

    tools.add(buildTool("aem_groups_list", "List groups",
        Map.of("limit", prop("integer", "Max results (default 50)", false))));

    tools.add(buildTool("aem_groups_members", "Get group members",
        Map.of("groupId", prop("string", "Group ID", true))));

    tools.add(buildTool("aem_users_add_to_group", "Add user to group",
        Map.of(
            "userId", prop("string", "User ID", true),
            "groupId", prop("string", "Group ID", true)
        )));

    // Replication tools
    tools.add(buildTool("aem_replicate_activate", "Publish content",
        Map.of("path", prop("string", "Content path", true))));

    tools.add(buildTool("aem_replicate_deactivate", "Unpublish content",
        Map.of("path", prop("string", "Content path", true))));

    tools.add(buildTool("aem_replicate_status", "Get publish status",
        Map.of("path", prop("string", "Content path", true))));

    // GraphQL tools
    tools.add(buildTool("aem_graphql_execute", "Execute GraphQL query",
        Map.of(
            "query", prop("string", "GraphQL query", true),
            "variables", prop("object", "Variables (optional)", false)
        )));

    tools.add(buildTool("aem_graphql_persisted", "Execute persisted query",
        Map.of(
            "endpoint", prop("string", "Persisted query endpoint", true),
            "variables", prop("object", "Variables (optional)", false)
        )));

    // Page tools
    tools.add(buildTool("aem_pages_list", "List pages",
        Map.of(
            "path", prop("string", "Parent page path", true),
            "limit", prop("integer", "Max results (default 50)", false)
        )));

    tools.add(buildTool("aem_pages_get", "Get page details",
        Map.of("path", prop("string", "Page path", true))));

    tools.add(buildTool("aem_pages_create", "Create a page",
        Map.of(
            "parentPath", prop("string", "Parent path", true),
            "name", prop("string", "Page name", true),
            "title", prop("string", "Page title", true),
            "template", prop("string", "Template path", true)
        )));

    tools.add(buildTool("aem_pages_delete", "Delete a page",
        Map.of("path", prop("string", "Page path", true))));

    // Package tools
    tools.add(buildTool("aem_packages_list", "List packages",
        Map.of("group", prop("string", "Package group (optional)", false))));

    tools.add(buildTool("aem_packages_build", "Build a package",
        Map.of(
            "group", prop("string", "Package group", true),
            "name", prop("string", "Package name", true)
        )));

    tools.add(buildTool("aem_packages_install", "Install a package",
        Map.of(
            "group", prop("string", "Package group", true),
            "name", prop("string", "Package name", true)
        )));

    return Map.of("tools", tools);
  }

  private ObjectNode buildTool(String name, String description, Map<String, Object> properties) {
    ObjectNode tool = mapper.createObjectNode();
    tool.put("name", name);
    tool.put("description", description);

    ObjectNode schema = mapper.createObjectNode();
    schema.put("type", "object");

    ObjectNode propsNode = mapper.createObjectNode();
    ArrayNode requiredNode = mapper.createArrayNode();

    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      @SuppressWarnings("unchecked")
      Map<String, Object> propDef = (Map<String, Object>) entry.getValue();
      ObjectNode propNode = mapper.createObjectNode();
      propNode.put("type", (String) propDef.get("type"));
      propNode.put("description", (String) propDef.get("description"));
      propsNode.set(entry.getKey(), propNode);

      if (Boolean.TRUE.equals(propDef.get("required"))) {
        requiredNode.add(entry.getKey());
      }
    }

    schema.set("properties", propsNode);
    schema.set("required", requiredNode);
    tool.set("inputSchema", schema);

    return tool;
  }

  private Map<String, Object> prop(String type, String description, boolean required) {
    return Map.of("type", type, "description", description, "required", required);
  }

  private Object handleToolsCall(JsonNode params) throws Exception {
    initializeApis();

    String name = params.path("name").asText();
    JsonNode args = params.get("arguments");
    if (args == null) {
      args = mapper.createObjectNode();
    }

    Object result = switch (name) {
      case "aem_connect" -> handleConnect(args);
      case "aem_status" -> handleStatus();
      case "aem_workflow_list" -> handleWorkflowList(args);
      case "aem_workflow_start" -> handleWorkflowStart(args);
      case "aem_workflow_terminate" -> handleWorkflowTerminate(args);
      case "aem_workflow_models" -> handleWorkflowModels();
      case "aem_workflow_stats" -> handleWorkflowStats();
      case "aem_assets_list" -> handleAssetsList(args);
      case "aem_assets_get" -> handleAssetsGet(args);
      case "aem_assets_delete" -> handleAssetsDelete(args);
      case "aem_assets_move" -> handleAssetsMove(args);
      case "aem_assets_search" -> handleAssetsSearch(args);
      case "aem_folder_create" -> handleFolderCreate(args);
      case "aem_cf_list" -> handleCfList(args);
      case "aem_cf_get" -> handleCfGet(args);
      case "aem_cf_create" -> handleCfCreate(args);
      case "aem_cf_delete" -> handleCfDelete(args);
      case "aem_cf_export" -> handleCfExport(args);
      case "aem_tags_list" -> handleTagsList(args);
      case "aem_tags_namespaces" -> handleTagsNamespaces();
      case "aem_tags_create" -> handleTagsCreate(args);
      case "aem_tags_delete" -> handleTagsDelete(args);
      case "aem_tags_apply" -> handleTagsApply(args);
      case "aem_tags_usage" -> handleTagsUsage(args);
      case "aem_users_list" -> handleUsersList(args);
      case "aem_users_get" -> handleUsersGet(args);
      case "aem_users_create" -> handleUsersCreate(args);
      case "aem_users_delete" -> handleUsersDelete(args);
      case "aem_groups_list" -> handleGroupsList(args);
      case "aem_groups_members" -> handleGroupsMembers(args);
      case "aem_users_add_to_group" -> handleUsersAddToGroup(args);
      case "aem_replicate_activate" -> handleReplicateActivate(args);
      case "aem_replicate_deactivate" -> handleReplicateDeactivate(args);
      case "aem_replicate_status" -> handleReplicateStatus(args);
      case "aem_graphql_execute" -> handleGraphqlExecute(args);
      case "aem_graphql_persisted" -> handleGraphqlPersisted(args);
      case "aem_pages_list" -> handlePagesList(args);
      case "aem_pages_get" -> handlePagesGet(args);
      case "aem_pages_create" -> handlePagesCreate(args);
      case "aem_pages_delete" -> handlePagesDelete(args);
      case "aem_packages_list" -> handlePackagesList(args);
      case "aem_packages_build" -> handlePackagesBuild(args);
      case "aem_packages_install" -> handlePackagesInstall(args);
      default -> throw new IllegalArgumentException("Unknown tool: " + name);
    };

    String json = mapper.writeValueAsString(result);
    return Map.of(
        "content", List.of(Map.of("type", "text", "text", json)),
        "isError", false
    );
  }

  // === Response Helpers ===

  private ObjectNode createSuccessResponse(JsonNode id, Object result) {
    ObjectNode response = mapper.createObjectNode();
    response.put("jsonrpc", "2.0");
    if (id != null) {
      response.set("id", id);
    }
    response.set("result", mapper.valueToTree(result));
    return response;
  }

  private ObjectNode createErrorResponse(JsonNode id, int code, String message) {
    ObjectNode response = mapper.createObjectNode();
    response.put("jsonrpc", "2.0");
    if (id != null) {
      response.set("id", id);
    }
    ObjectNode error = mapper.createObjectNode();
    error.put("code", code);
    error.put("message", message);
    response.set("error", error);
    return response;
  }

  // === Tool Handlers ===

  private Object handleConnect(JsonNode args) {
    String env = args.path("environment").asText("default");
    String url = args.path("url").asText(null);
    String username = args.path("username").asText(null);
    String password = args.path("password").asText(null);

    ConfigManager config = ConfigManager.getInstance();

    if (url != null) {
      config.setEnvironmentUrl(env, url);
    }
    if (username != null && password != null) {
      String basicAuth = java.util.Base64.getEncoder()
          .encodeToString((username + ":" + password).getBytes());
      config.setBasicAuth(env, basicAuth);
    }
    config.setActiveEnvironment(env);
    config.save();

    client = new AemApiClient();
    initializeApis();

    return Map.of(
        "status", "connected",
        "environment", env,
        "url", nullSafe(config.getActiveEnvironmentUrl())
    );
  }

  private Object handleStatus() {
    ConfigManager config = ConfigManager.getInstance();
    return Map.of(
        "connected", config.getActiveEnvironmentUrl() != null,
        "environment", nullSafe(config.getActiveEnvironment()),
        "url", nullSafe(config.getActiveEnvironmentUrl())
    );
  }

  private Object handleWorkflowList(JsonNode args) throws Exception {
    String status = args.path("status").asText(null);
    int limit = args.path("limit").asInt(50);

    WorkflowApi.WorkflowStatus ws = status != null
        ? WorkflowApi.WorkflowStatus.valueOf(status) : null;
    List<WorkflowApi.WorkflowInstance> instances = workflowApi.listInstances(ws, limit);

    return Map.of(
        "count", instances.size(),
        "workflows", instances.stream().map(i -> Map.of(
            "id", nullSafe(i.getId()),
            "model", nullSafe(i.getModelTitle()),
            "status", nullSafe(i.getStatus()),
            "payload", nullSafe(i.getPayload()),
            "initiator", nullSafe(i.getInitiator()),
            "currentStep", nullSafe(i.getCurrentStep())
        )).toList()
    );
  }

  private Object handleWorkflowStart(JsonNode args) throws Exception {
    String model = args.path("model").asText();
    String payload = args.path("payload").asText();

    WorkflowApi.WorkflowInstance instance = workflowApi.startWorkflow(model, payload);
    return Map.of(
        "success", true,
        "instanceId", nullSafe(instance.getId()),
        "status", nullSafe(instance.getStatus())
    );
  }

  private Object handleWorkflowTerminate(JsonNode args) throws Exception {
    String instanceId = args.path("instanceId").asText();
    boolean success = workflowApi.terminateWorkflow(instanceId);
    return Map.of("success", success, "instanceId", instanceId);
  }

  private Object handleWorkflowModels() throws Exception {
    List<WorkflowApi.WorkflowModel> models = workflowApi.listModels();
    return Map.of(
        "count", models.size(),
        "models", models.stream().map(m -> Map.of(
            "path", nullSafe(m.getPath()),
            "title", nullSafe(m.getTitle()),
            "description", nullSafe(m.getDescription()),
            "version", nullSafe(m.getVersion())
        )).toList()
    );
  }

  private Object handleWorkflowStats() throws Exception {
    WorkflowApi.WorkflowStats stats = workflowApi.getStatistics();
    return Map.of(
        "running", stats.getRunning(),
        "completed", stats.getCompleted(),
        "suspended", stats.getSuspended(),
        "aborted", stats.getAborted(),
        "stale", stats.getStale(),
        "pendingWorkItems", stats.getPendingWorkItems(),
        "availableModels", stats.getAvailableModels(),
        "total", stats.getTotal()
    );
  }

  private Object handleAssetsList(JsonNode args) throws Exception {
    String path = args.path("path").asText();
    int limit = args.path("limit").asInt(50);

    List<AssetsApi.Asset> assets = assetsApi.list(path, limit);
    return Map.of(
        "count", assets.size(),
        "path", path,
        "assets", assets.stream().map(a -> Map.of(
            "name", nullSafe(a.getName()),
            "title", nullSafe(a.getTitle()),
            "path", nullSafe(a.getPath()),
            "mimeType", nullSafe(a.getMimeType()),
            "modified", nullSafe(a.getModified())
        )).toList()
    );
  }

  private Object handleAssetsGet(JsonNode args) throws Exception {
    String path = args.path("path").asText();
    AssetsApi.Asset asset = assetsApi.get(path);
    return Map.of(
        "name", nullSafe(asset.getName()),
        "title", nullSafe(asset.getTitle()),
        "path", nullSafe(asset.getPath()),
        "mimeType", nullSafe(asset.getMimeType()),
        "description", nullSafe(asset.getDescription()),
        "created", nullSafe(asset.getCreated()),
        "modified", nullSafe(asset.getModified())
    );
  }

  private Object handleAssetsDelete(JsonNode args) throws Exception {
    String path = args.path("path").asText();
    boolean success = assetsApi.delete(path);
    return Map.of("success", success, "path", path);
  }

  private Object handleAssetsMove(JsonNode args) throws Exception {
    String source = args.path("source").asText();
    String destination = args.path("destination").asText();
    assetsApi.move(source, destination);
    return Map.of("success", true, "source", source, "destination", destination);
  }

  private Object handleAssetsSearch(JsonNode args) throws Exception {
    String query = args.path("query").asText();
    int limit = args.path("limit").asInt(20);

    List<AssetsApi.Asset> assets = assetsApi.search(query, limit);
    return Map.of(
        "query", query,
        "count", assets.size(),
        "results", assets.stream().map(a -> Map.of(
            "name", nullSafe(a.getName()),
            "path", nullSafe(a.getPath()),
            "mimeType", nullSafe(a.getMimeType())
        )).toList()
    );
  }

  private Object handleFolderCreate(JsonNode args) throws Exception {
    String parentPath = args.path("parentPath").asText();
    String name = args.path("name").asText();
    String title = args.path("title").asText(null);

    AssetsApi.Folder folder = assetsApi.createFolder(parentPath, name, title);
    return Map.of(
        "success", true,
        "name", nullSafe(folder.getName()),
        "path", nullSafe(folder.getPath())
    );
  }

  private Object handleCfList(JsonNode args) throws Exception {
    String path = args.path("path").asText();
    int limit = args.path("limit").asInt(50);

    List<ContentFragmentApi.ContentFragment> fragments = contentFragmentApi.list(path, limit);
    return Map.of(
        "count", fragments.size(),
        "path", path,
        "fragments", fragments.stream().map(cf -> Map.of(
            "name", nullSafe(cf.getName()),
            "title", nullSafe(cf.getTitle()),
            "path", nullSafe(cf.getPath()),
            "model", nullSafe(cf.getModel()),
            "modified", nullSafe(cf.getModified())
        )).toList()
    );
  }

  private Object handleCfGet(JsonNode args) throws Exception {
    String path = args.path("path").asText();
    ContentFragmentApi.ContentFragment cf = contentFragmentApi.get(path);
    return Map.of(
        "name", nullSafe(cf.getName()),
        "title", nullSafe(cf.getTitle()),
        "path", nullSafe(cf.getPath()),
        "model", nullSafe(cf.getModel()),
        "description", nullSafe(cf.getDescription()),
        "created", nullSafe(cf.getCreated()),
        "modified", nullSafe(cf.getModified())
    );
  }

  private Object handleCfCreate(JsonNode args) throws Exception {
    String parentPath = args.path("parentPath").asText();
    String name = args.path("name").asText();
    String model = args.path("model").asText();
    String title = args.path("title").asText(null);

    ContentFragmentApi.ContentFragment cf = contentFragmentApi.create(
        parentPath, name, model, title);
    return Map.of(
        "success", true,
        "name", nullSafe(cf.getName()),
        "path", nullSafe(cf.getPath())
    );
  }

  private Object handleCfDelete(JsonNode args) throws Exception {
    String path = args.path("path").asText();
    boolean success = contentFragmentApi.delete(path);
    return Map.of("success", success, "path", path);
  }

  private Object handleCfExport(JsonNode args) throws Exception {
    String path = args.path("path").asText();
    int limit = args.path("limit").asInt(100);

    String json = contentFragmentApi.exportToJson(path, limit);
    return Map.of("path", path, "export", mapper.readTree(json));
  }

  private Object handleTagsList(JsonNode args) throws Exception {
    String path = args.path("path").asText("/content/cq:tags");
    boolean recursive = args.path("recursive").asBoolean(false);
    int limit = args.path("limit").asInt(100);

    List<TagsApi.Tag> tags = tagsApi.listTags(path, recursive, limit);
    return Map.of(
        "count", tags.size(),
        "tags", tags.stream().map(t -> Map.of(
            "tagId", nullSafe(t.tagId()),
            "title", nullSafe(t.title()),
            "namespace", nullSafe(t.namespace()),
            "childCount", t.childCount()
        )).toList()
    );
  }

  private Object handleTagsNamespaces() throws Exception {
    List<TagsApi.TagNamespace> namespaces = tagsApi.listNamespaces();
    return Map.of(
        "count", namespaces.size(),
        "namespaces", namespaces.stream().map(ns -> Map.of(
            "id", nullSafe(ns.id()),
            "title", nullSafe(ns.title()),
            "path", nullSafe(ns.path()),
            "tagCount", ns.tagCount()
        )).toList()
    );
  }

  private Object handleTagsCreate(JsonNode args) throws Exception {
    String tagId = args.path("tagId").asText();
    String title = args.path("title").asText();
    String description = args.path("description").asText(null);

    TagsApi.Tag tag = tagsApi.createTag(tagId, title, description);
    return Map.of(
        "success", true,
        "tagId", nullSafe(tag.tagId()),
        "path", nullSafe(tag.path())
    );
  }

  private Object handleTagsDelete(JsonNode args) throws Exception {
    String tagId = args.path("tagId").asText();
    boolean force = args.path("force").asBoolean(false);

    boolean success = tagsApi.deleteTag(tagId, force);
    return Map.of("success", success, "tagId", tagId);
  }

  private Object handleTagsApply(JsonNode args) throws Exception {
    String contentPath = args.path("contentPath").asText();
    List<String> tags = new java.util.ArrayList<>();
    if (args.has("tags") && args.get("tags").isArray()) {
      for (JsonNode tag : args.get("tags")) {
        tags.add(tag.asText());
      }
    }
    boolean replace = args.path("replace").asBoolean(false);

    boolean success = tagsApi.applyTags(contentPath, tags, replace);
    return Map.of("success", success, "contentPath", contentPath, "tagsApplied", tags.size());
  }

  private Object handleTagsUsage(JsonNode args) throws Exception {
    String tagId = args.path("tagId").asText();
    int count = tagsApi.getTagUsageCount(tagId);
    return Map.of("tagId", tagId, "usageCount", count);
  }

  private Object handleUsersList(JsonNode args) throws Exception {
    String path = args.path("path").asText("/home/users");
    int limit = args.path("limit").asInt(50);

    List<UsersApi.User> users = usersApi.listUsers(path, limit);
    return Map.of(
        "count", users.size(),
        "users", users.stream().map(u -> Map.of(
            "id", nullSafe(u.id()),
            "displayName", nullSafe(u.displayName()),
            "email", nullSafe(u.email()),
            "disabled", u.disabled()
        )).toList()
    );
  }

  private Object handleUsersGet(JsonNode args) throws Exception {
    String userId = args.path("userId").asText();
    UsersApi.User user = usersApi.getUser(userId);
    List<String> groups = usersApi.getUserGroups(userId);

    return Map.of(
        "id", nullSafe(user.id()),
        "displayName", nullSafe(user.displayName()),
        "email", nullSafe(user.email()),
        "givenName", nullSafe(user.givenName()),
        "familyName", nullSafe(user.familyName()),
        "disabled", user.disabled(),
        "groups", groups
    );
  }

  private Object handleUsersCreate(JsonNode args) throws Exception {
    String userId = args.path("userId").asText();
    String password = args.path("password").asText();
    String email = args.path("email").asText(null);
    String givenName = args.path("givenName").asText(null);
    String familyName = args.path("familyName").asText(null);

    UsersApi.User user = usersApi.createUser(userId, password, email, givenName, familyName);
    return Map.of(
        "success", true,
        "id", nullSafe(user.id()),
        "path", nullSafe(user.path())
    );
  }

  private Object handleUsersDelete(JsonNode args) throws Exception {
    String userId = args.path("userId").asText();
    boolean success = usersApi.deleteUser(userId);
    return Map.of("success", success, "userId", userId);
  }

  private Object handleGroupsList(JsonNode args) throws Exception {
    int limit = args.path("limit").asInt(50);
    List<UsersApi.Group> groups = usersApi.listGroups(null, limit);

    return Map.of(
        "count", groups.size(),
        "groups", groups.stream().map(g -> Map.of(
            "id", nullSafe(g.id()),
            "displayName", nullSafe(g.displayName()),
            "memberCount", g.memberCount()
        )).toList()
    );
  }

  private Object handleGroupsMembers(JsonNode args) throws Exception {
    String groupId = args.path("groupId").asText();
    List<String> members = usersApi.getGroupMembers(groupId);
    return Map.of("groupId", groupId, "memberCount", members.size(), "members", members);
  }

  private Object handleUsersAddToGroup(JsonNode args) throws Exception {
    String userId = args.path("userId").asText();
    String groupId = args.path("groupId").asText();

    boolean success = usersApi.addUserToGroup(userId, groupId);
    return Map.of("success", success, "userId", userId, "groupId", groupId);
  }

  private Object handleReplicateActivate(JsonNode args) throws Exception {
    String path = args.path("path").asText();
    boolean success = replicationApi.publish(path, null);
    return Map.of("success", success, "path", path, "action", "activate");
  }

  private Object handleReplicateDeactivate(JsonNode args) throws Exception {
    String path = args.path("path").asText();
    boolean success = replicationApi.unpublish(path, null);
    return Map.of("success", success, "path", path, "action", "deactivate");
  }

  private Object handleReplicateStatus(JsonNode args) throws Exception {
    String path = args.path("path").asText();
    ReplicationApi.ReplicationStatus status = replicationApi.getStatus(path);
    return Map.of(
        "path", path,
        "published", status.isPublished(),
        "lastPublished", nullSafe(status.getLastPublished()),
        "replicationCount", status.getReplicationCount()
    );
  }

  private Object handleGraphqlExecute(JsonNode args) throws Exception {
    String query = args.path("query").asText();
    JsonNode result = graphqlApi.executeQuery(query);
    return mapper.convertValue(result, Map.class);
  }

  private Object handleGraphqlPersisted(JsonNode args) throws Exception {
    String endpoint = args.path("endpoint").asText();
    JsonNode result = graphqlApi.executePersistedQuery(endpoint);
    return mapper.convertValue(result, Map.class);
  }

  private Object handlePagesList(JsonNode args) throws Exception {
    String path = args.path("path").asText();
    int limit = args.path("limit").asInt(50);

    List<PagesApi.Page> pages = pagesApi.list(path, limit);
    return Map.of(
        "count", pages.size(),
        "path", path,
        "pages", pages.stream().map(p -> Map.of(
            "name", nullSafe(p.getName()),
            "title", nullSafe(p.getTitle()),
            "path", nullSafe(p.getPath()),
            "template", nullSafe(p.getTemplate())
        )).toList()
    );
  }

  private Object handlePagesGet(JsonNode args) throws Exception {
    String path = args.path("path").asText();
    PagesApi.Page page = pagesApi.get(path);
    return Map.of(
        "name", nullSafe(page.getName()),
        "title", nullSafe(page.getTitle()),
        "path", nullSafe(page.getPath()),
        "template", nullSafe(page.getTemplate()),
        "created", nullSafe(page.getCreated()),
        "modified", nullSafe(page.getModified())
    );
  }

  private Object handlePagesCreate(JsonNode args) throws Exception {
    String parentPath = args.path("parentPath").asText();
    String name = args.path("name").asText();
    String title = args.path("title").asText();
    String template = args.path("template").asText();

    PagesApi.Page page = pagesApi.create(parentPath, name, title, template);
    return Map.of(
        "success", true,
        "name", nullSafe(page.getName()),
        "path", nullSafe(page.getPath())
    );
  }

  private Object handlePagesDelete(JsonNode args) throws Exception {
    String path = args.path("path").asText();
    boolean success = pagesApi.delete(path);
    return Map.of("success", success, "path", path);
  }

  private Object handlePackagesList(JsonNode args) throws Exception {
    String group = args.path("group").asText(null);
    List<PackagesApi.Package> packages = packagesApi.list(group);
    return Map.of(
        "count", packages.size(),
        "packages", packages.stream().map(p -> Map.of(
            "name", nullSafe(p.getName()),
            "group", nullSafe(p.getGroup()),
            "version", nullSafe(p.getVersion()),
            "size", p.getSize()
        )).toList()
    );
  }

  private Object handlePackagesBuild(JsonNode args) throws Exception {
    String group = args.path("group").asText();
    String name = args.path("name").asText();
    boolean success = packagesApi.build(group, name);
    return Map.of("success", success, "group", group, "name", name, "action", "build");
  }

  private Object handlePackagesInstall(JsonNode args) throws Exception {
    String group = args.path("group").asText();
    String name = args.path("name").asText();
    boolean success = packagesApi.install(group, name);
    return Map.of("success", success, "group", group, "name", name, "action", "install");
  }

  private String nullSafe(String value) {
    return value != null ? value : "";
  }
}
