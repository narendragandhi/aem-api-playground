package com.aemtools.aem;

import com.aemtools.aem.agent.AemAgent;
import com.aemtools.aem.agent.AgentMemory;
import com.aemtools.aem.client.AemApiClient;
import com.aemtools.aem.config.ConfigManager;
import com.aemtools.aem.config.LoggerManager;
import com.aemtools.aem.api.AssetsApi;
import com.aemtools.aem.api.ContentFragmentApi;
import com.aemtools.aem.security.InputValidator;
import com.aemtools.aem.shell.InteractiveShell;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import com.aemtools.aem.util.OutputHelper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(
    name = "aem-api",
    description = "AEM API Playground - Interactive CLI for testing Adobe Experience Manager APIs",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    sortSynopsis = false,
    subcommands = {
        AemApi.ShellCommand.class,
        AemApi.ConnectCommand.class,
        AemApi.ContentFragmentCommand.class,
        AemApi.AssetsCommand.class,
        AemApi.SitesCommand.class,
        AemApi.FormsCommand.class,
        AemApi.ConfigCommand.class,
        AemApi.GraphQLCommand.class,
        AemApi.TranslationCommand.class,
        AemApi.CloudManagerCommand.class,
        AemApi.FoldersCommand.class,
        AemApi.TagsCommand.class,
        AemApi.WorkflowCommand.class,
        AemApi.UsersCommand.class,
        AemApi.ReplicationCommand.class,
        AemApi.PackagesCommand.class,
        AemApi.ModelsCommand.class,
        AemApi.AuditCommand.class,
        AemApi.AgentCommand.class,
        AemApi.CompletionCommand.class
    }
)
public class AemApi implements Callable<Integer> {

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    private boolean verbose;

    @Option(names = {"--debug"}, description = "Enable debug mode (show HTTP requests/responses)")
    private boolean debug;

    @Option(names = {"--log-level", "--logLevel"}, 
            description = "Log level: silly, debug, verbose, info, warn, error",
            defaultValue = "info",
            order = 0)
    private String logLevel;

    @Option(names = {"--log-file", "--logFile"}, 
            description = "Log file path (use '-' for stdout, empty for no file)",
            defaultValue = "-",
            order = 1)
    private String logFile;

    @Option(names = {"--http-proxy", "--proxy"}, 
            description = "HTTP proxy URL (e.g., http://proxy:8080)",
            order = 2)
    private String httpProxy;

    @Option(names = {"--https-proxy", "--httpsProxy"}, 
            description = "HTTPS proxy URL",
            order = 3)
    private String httpsProxy;

    @Option(names = {"--no-proxy", "--noProxy"}, 
            description = "Comma-separated list of hosts to bypass proxy",
            order = 4)
    private String noProxy;

    @Option(names = {"--env-file"}, 
            description = "Path to .env file (default: .env in current directory)",
            order = 5)
    private String envFile;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AemApi())
            .setCaseInsensitiveEnumValuesAllowed(true)
            .execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        LoggerManager loggerMgr = LoggerManager.getInstance();
        
        if (envFile != null && !envFile.isEmpty()) {
            java.nio.file.Path p = java.nio.file.Paths.get(envFile);
            if (java.nio.file.Files.exists(p)) {
                loggerMgr.loadEnvFile(p.getParent() != null ? p.getParent().toString() : ".");
            }
        } else {
            loggerMgr.loadEnvFile(".");
        }

        String effectiveLogLevel = loggerMgr.getEnv("AEM_LOG_LEVEL", logLevel);
        String effectiveLogFile = loggerMgr.getEnv("AEM_LOG_FILE", logFile);
        loggerMgr.configureLogging(effectiveLogLevel, effectiveLogFile);

        System.out.println("AEM API Playground v1.0.0");
        System.out.println("Log level: " + effectiveLogLevel);
        System.out.println("Type 'shell' to enter interactive mode or use subcommands.");
        System.out.println("Type 'help' for available commands.");
        return 0;
    }

    @Command(name = "shell", description = "Enter interactive shell mode")
    public static class ShellCommand implements Callable<Integer> {
        @Option(names = {"--history-file"}, description = "Path to history file")
        private String historyFile = "~/.aem-api/history.txt";

        @Override
        public Integer call() throws Exception {
            InteractiveShell shell = new InteractiveShell(historyFile);
            return shell.run();
        }
    }

    @Command(name = "connect", description = "Connect to an AEM environment")
    public static class ConnectCommand implements Callable<Integer> {
        @Option(names = {"-e", "--env"}, description = "Environment name (dev, staging, prod, local)", required = true)
        private String env;

        @Option(names = {"-u", "--url"}, description = "AEM host URL")
        private String url;

        @Option(names = {"--local"}, description = "Connect to local AEM SDK (http://localhost:4502)")
        private boolean local;

        @Option(names = {"--https-only"}, description = "Enforce HTTPS (reject HTTP connections)")
        private boolean httpsOnly;

        @Option(names = {"--ims-endpoint"}, description = "Adobe IMS endpoint")
        private String imsEndpoint = "https://ims-na1.adobelogin.com/ims/token/v3";

        @Option(names = {"--client-id"}, description = "Adobe IO Client ID")
        private String clientId;

        @Option(names = {"--client-secret"}, description = "Adobe IO Client Secret")
        private String clientSecret;

        @Option(names = {"--access-token"}, description = "Direct access token (bypasses OAuth flow)")
        private String accessToken;

        @Option(names = {"-u2", "--user"}, description = "Username for basic auth (local AEM)")
        private String username;

        @Option(names = {"-p2", "--password"}, description = "Password for basic auth (local AEM)")
        private String password;

        @Option(names = {"--save"}, description = "Save configuration for future use")
        private boolean save;

        @Override
        public Integer call() throws Exception {
            ConfigManager config = ConfigManager.getInstance();
            
            if (local) {
                url = "http://localhost:4502";
                System.out.println("Using local AEM SDK: " + url);
            }
            
            if (url == null || url.isEmpty()) {
                System.out.println("Error: --url or --local required");
                return 1;
            }
            
            if (httpsOnly && url.startsWith("http://")) {
                System.out.println("Error: HTTPS enforcement enabled. Use HTTPS URL.");
                return 1;
            }
            
            if (username != null && password != null) {
                String encoded = java.util.Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes());
                config.setBasicAuth(env, encoded);
                System.out.println("Using basic auth for: " + env);
            }
            
            if (save) {
                config.setActiveEnvironment(env);
                config.setEnvironmentUrl(env, url);
                if (accessToken != null) {
                    config.setAccessToken(env, accessToken);
                }
                if (username != null && password != null) {
                    String encoded = java.util.Base64.getEncoder()
                        .encodeToString((username + ":" + password).getBytes());
                    config.setBasicAuth(env, encoded);
                }
                config.save();
                System.out.println("Configuration saved for environment: " + env);
            } else if (accessToken != null) {
                config.setActiveEnvironment(env);
                config.setEnvironmentUrl(env, url);
                config.setAccessToken(env, accessToken);
                System.out.println("Connected to " + env + " (" + url + ") with access token");
            } else if (username != null && password != null) {
                config.setActiveEnvironment(env);
                config.setEnvironmentUrl(env, url);
                config.setBasicAuth(env, java.util.Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes()));
                System.out.println("Connected to " + env + " (" + url + ") with basic auth");
            } else {
                config.setActiveEnvironment(env);
                config.setEnvironmentUrl(env, url);
                System.out.println("Connected to " + env + " (" + url + ")");
            }
            return 0;
        }
    }

    @Command(name = "cf", description = "Content Fragment operations", subcommands = {
        ContentFragmentCommand.ListCommand.class,
        ContentFragmentCommand.GetCommand.class,
        ContentFragmentCommand.CreateCommand.class
    })
    public static class ContentFragmentCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'cf list', 'cf get', or 'cf create' for operations");
            return 0;
        }

        @Command(name = "list", description = "List content fragments")
        public static class ListCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Path to content fragments", defaultValue = "/content/dam")
            private String path;

            @Option(names = {"-m", "--max"}, description = "Max results", defaultValue = "20")
            private int max;

            @Option(names = {"--format"}, description = "Output format: table, json, raw", defaultValue = "table")
            private String format;

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

        @Command(name = "get", description = "Get a content fragment by path")
        public static class GetCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Content fragment path", required = true)
            private String path;

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
    }

    @Command(name = "assets", description = "Assets operations", subcommands = {
        AssetsCommand.ListCommand.class,
        AssetsCommand.UploadCommand.class,
        AssetsCommand.DeleteCommand.class
    })
    public static class AssetsCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'assets list', 'assets upload', or 'assets delete' for operations");
            return 0;
        }

        @Command(name = "list", description = "List assets")
        public static class ListCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Folder path", defaultValue = "/content/dam")
            private String path;

            @Option(names = {"-m", "--max"}, description = "Max results", defaultValue = "20")
            private int max;

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

        @Command(name = "upload", description = "Upload an asset")
        public static class UploadCommand implements Callable<Integer> {
            @Option(names = {"-f", "--file"}, description = "File to upload", required = true)
            private String filePath;

            @Option(names = {"-p", "--path"}, description = "Destination folder", defaultValue = "/content/dam")
            private String folderPath;

            @Override
            public Integer call() throws Exception {
                ConfigManager config = ConfigManager.getInstance();
                String baseUrl = config.getActiveEnvironmentUrl();
                
                if (baseUrl == null || baseUrl.isEmpty()) {
                    System.out.println("Not connected. Run 'connect --env <env> --url <url>' first.");
                    return 1;
                }

                try {
                    java.nio.file.Path path = java.nio.file.Paths.get(filePath);
                    if (!java.nio.file.Files.exists(path)) {
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

        @Command(name = "delete", description = "Delete an asset")
        public static class DeleteCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Asset path", required = true)
            private String path;

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

    @Command(name = "sites", description = "Sites operations", subcommands = {
        SitesCommand.ListCommand.class,
        SitesCommand.PagesCommand.class
    })
    public static class SitesCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'sites list' or 'sites pages' for operations");
            return 0;
        }

        @Command(name = "list", description = "List sites")
        public static class ListCommand implements Callable<Integer> {
            @Override
            public Integer call() throws Exception {
                System.out.println("(Demo mode - API client not fully implemented)");
                return 0;
            }
        }

        @Command(name = "pages", description = "List pages in a site")
        public static class PagesCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Site path")
            private String path;

            @Override
            public Integer call() throws Exception {
                System.out.println("Listing pages in: " + (path != null ? path : "/content"));
                System.out.println("(Demo mode - API client not fully implemented)");
                return 0;
            }
        }
    }

    @Command(name = "forms", description = "Forms operations", subcommands = {
        FormsCommand.ListCommand.class,
        FormsCommand.SubmitCommand.class
    })
    public static class FormsCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'forms list' or 'forms submit' for operations");
            return 0;
        }

        @Command(name = "list", description = "List adaptive forms")
        public static class ListCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Forms folder path", defaultValue = "/content/forms/af")
            private String path;

            @Override
            public Integer call() throws Exception {
                System.out.println("Listing forms in: " + path);
                System.out.println("(Demo mode - API client not fully implemented)");
                return 0;
            }
        }

        @Command(name = "submit", description = "Submit a form (for testing)")
        public static class SubmitCommand implements Callable<Integer> {
            @Option(names = {"-f", "--form"}, description = "Form path", required = true)
            private String formPath;

            @Option(names = {"-d", "--data"}, description = "JSON form data", required = true)
            private String data;

            @Override
            public Integer call() throws Exception {
                System.out.println("Submitting form: " + formPath);
                System.out.println("Data: " + data);
                System.out.println("(Demo mode - API client not fully implemented)");
                return 0;
            }
        }
    }

    @Command(name = "config", description = "Configuration management", subcommands = {
        ConfigCommand.ShowCommand.class,
        ConfigCommand.EnvCommand.class
    })
    public static class ConfigCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'config show' or 'config env' for operations");
            return 0;
        }

        @Command(name = "show", description = "Show current configuration")
        public static class ShowCommand implements Callable<Integer> {
            @Override
            public Integer call() throws Exception {
                ConfigManager config = ConfigManager.getInstance();
                config.showConfig();
                return 0;
            }
        }

        @Command(name = "env", description = "Manage environments")
        public static class EnvCommand implements Callable<Integer> {
            @Option(names = {"-s", "--set"}, description = "Set active environment")
            private String setEnv;

            @Option(names = {"-l", "--list"}, description = "List all environments")
            private boolean list;

            @Option(names = {"--save"}, description = "Save configuration to disk")
            private boolean save;

            @Override
            public Integer call() throws Exception {
                ConfigManager config = ConfigManager.getInstance();
                if (list) {
                    config.listEnvironments();
                } else if (setEnv != null) {
                    config.setActiveEnvironment(setEnv);
                    System.out.println("Switched to environment: " + setEnv);
                    if (save) {
                        config.save();
                        System.out.println("Configuration saved.");
                    }
                } else {
                    System.out.println("Current environment: " + config.getActiveEnvironment());
                }
                return 0;
            }
        }
    }

    @Command(name = "graphql", description = "GraphQL operations", subcommands = {
        GraphQLCommand.QueryCommand.class,
        GraphQLCommand.PersistedCommand.class,
        GraphQLCommand.ListCommand.class
    })
    public static class GraphQLCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'graphql query', 'graphql persisted', or 'graphql list' for operations");
            return 0;
        }

        @Command(name = "query", description = "Execute a GraphQL query")
        public static class QueryCommand implements Callable<Integer> {
            @Option(names = {"-q", "--query"}, description = "GraphQL query string", required = true)
            private String query;

            @Option(names = {"-v", "--variables"}, description = "JSON variables")
            private String variables;

            @Override
            public Integer call() throws Exception {
                ConfigManager config = ConfigManager.getInstance();
                String baseUrl = config.getActiveEnvironmentUrl();
                
                if (baseUrl == null || baseUrl.isEmpty()) {
                    System.out.println("Not connected. Run 'connect' first.");
                    return 1;
                }

                try {
                    AemApiClient client = new AemApiClient();
                    
                    ObjectNode request = client.getObjectMapper().createObjectNode();
                    request.put("query", query);
                    if (variables != null) {
                        request.set("variables", client.getObjectMapper().readTree(variables));
                    }
                    
                    JsonNode response = client.post("/graphql/execute.json", request);
                    
                    System.out.println("\nGraphQL Response:");
                    System.out.println(response.toString());
                    return 0;
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    return 1;
                }
            }
        }

        @Command(name = "persisted", description = "Execute a persisted GraphQL query")
        public static class PersistedCommand implements Callable<Integer> {
            @Option(names = {"-n", "--name"}, description = "Persisted query name", required = true)
            private String name;

            @Option(names = {"-v", "--variables"}, description = "JSON variables")
            private String variables;

            @Override
            public Integer call() throws Exception {
                ConfigManager config = ConfigManager.getInstance();
                String baseUrl = config.getActiveEnvironmentUrl();
                
                if (baseUrl == null || baseUrl.isEmpty()) {
                    System.out.println("Not connected. Run 'connect' first.");
                    return 1;
                }

                try {
                    AemApiClient client = new AemApiClient();
                    
                    String endpoint = "/graphql/execute.json/" + name;
                    ObjectNode request = client.getObjectMapper().createObjectNode();
                    if (variables != null) {
                        request.set("variables", client.getObjectMapper().readTree(variables));
                    }
                    
                    JsonNode response = client.post(endpoint, request);
                    
                    System.out.println("\nPersisted Query Response:");
                    System.out.println(response.toString());
                    return 0;
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    return 1;
                }
            }
        }

        @Command(name = "list", description = "List persisted GraphQL queries")
        public static class ListCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Folder path", defaultValue = "/graphql/persisted-query")
            private String path;

            @Override
            public Integer call() throws Exception {
                ConfigManager config = ConfigManager.getInstance();
                String baseUrl = config.getActiveEnvironmentUrl();
                
                if (baseUrl == null || baseUrl.isEmpty()) {
                    System.out.println("Not connected. Run 'connect' first.");
                    return 1;
                }

                try {
                    AemApiClient client = new AemApiClient();
                    JsonNode response = client.get(path + ".1.json");
                    
                    System.out.println("\nPersisted Queries:");
                    if (response.has("hits")) {
                        response.get("hits").forEach(hit -> {
                            System.out.println("  - " + hit.path("name").asText());
                        });
                    } else {
                        System.out.println("  (none found)");
                    }
                    return 0;
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    return 1;
                }
            }
        }
    }

    @Command(name = "translation", description = "Translation operations", subcommands = {
        TranslationCommand.ProjectsCommand.class,
        TranslationCommand.JobsCommand.class,
        TranslationCommand.SubmitCommand.class
    })
    public static class TranslationCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'translation projects', 'translation jobs', or 'translation submit' for operations");
            return 0;
        }

        @Command(name = "projects", description = "List translation projects")
        public static class ProjectsCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Projects path", defaultValue = "/content/projects")
            private String path;

            @Override
            public Integer call() throws Exception {
                System.out.println("Listing translation projects in: " + path);
                System.out.println("(Translation projects not fully implemented)");
                return 0;
            }
        }

        @Command(name = "jobs", description = "List translation jobs")
        public static class JobsCommand implements Callable<Integer> {
            @Option(names = {"-p", "--project"}, description = "Project name")
            private String project;

            @Override
            public Integer call() throws Exception {
                System.out.println("Listing translation jobs" + (project != null ? " for project: " + project : ""));
                System.out.println("(Translation jobs not fully implemented)");
                return 0;
            }
        }

        @Command(name = "submit", description = "Submit content for translation")
        public static class SubmitCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Content path to translate", required = true)
            private String path;

            @Option(names = {"-l", "--languages"}, description = "Target languages (comma-separated)", required = true)
            private String languages;

            @Option(names = {"-t", "--title"}, description = "Project title")
            private String title;

            @Override
            public Integer call() throws Exception {
                System.out.println("Submitting for translation:");
                System.out.println("  Path: " + path);
                System.out.println("  Languages: " + languages);
                System.out.println("  Title: " + (title != null ? title : "Auto-generated"));
                System.out.println("(Translation submission not fully implemented)");
                return 0;
            }
        }
    }

    @Command(name = "cloudmgr", description = "Cloud Manager API operations", subcommands = {
        CloudManagerCommand.ProgramsCommand.class,
        CloudManagerCommand.PipelinesCommand.class,
        CloudManagerCommand.ExecuteCommand.class
    })
    public static class CloudManagerCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'cloudmgr programs', 'cloudmgr pipelines', or 'cloudmgr execute' for operations");
            return 0;
        }

        @Command(name = "programs", description = "List Cloud Manager programs")
        public static class ProgramsCommand implements Callable<Integer> {
            @Override
            public Integer call() throws Exception {
                System.out.println("Listing Cloud Manager programs");
                System.out.println("(Requires Cloud Manager API credentials)");
                return 0;
            }
        }

        @Command(name = "pipelines", description = "List pipelines for a program")
        public static class PipelinesCommand implements Callable<Integer> {
            @Option(names = {"-p", "--program"}, description = "Program ID", required = true)
            private String programId;

            @Override
            public Integer call() throws Exception {
                System.out.println("Listing pipelines for program: " + programId);
                System.out.println("(Cloud Manager API not fully implemented)");
                return 0;
            }
        }

        @Command(name = "execute", description = "Execute a pipeline")
        public static class ExecuteCommand implements Callable<Integer> {
            @Option(names = {"-p", "--program"}, description = "Program ID", required = true)
            private String programId;

            @Option(names = {"-p2", "--pipeline"}, description = "Pipeline ID", required = true)
            private String pipelineId;

            @Override
            public Integer call() throws Exception {
                System.out.println("Executing pipeline: " + pipelineId);
                System.out.println("Program: " + programId);
                System.out.println("(Pipeline execution not fully implemented)");
                return 0;
            }
        }
    }

    @Command(name = "folders", description = "Folder operations", subcommands = {
        FoldersCommand.CreateCommand.class,
        FoldersCommand.DeleteCommand.class,
        FoldersCommand.ListCommand.class
    })
    public static class FoldersCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'folders create', 'folders delete', or 'folders list' for operations");
            return 0;
        }

        @Command(name = "create", description = "Create a folder")
        public static class CreateCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Parent path", required = true)
            private String parentPath;

            @Option(names = {"-n", "--name"}, description = "Folder name", required = true)
            private String name;

            @Option(names = {"-t", "--title"}, description = "Folder title")
            private String title;

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

        @Command(name = "delete", description = "Delete a folder")
        public static class DeleteCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Folder path", required = true)
            private String path;

            @Option(names = {"--force"}, description = "Force delete (with children)")
            private boolean force;

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

        @Command(name = "list", description = "List folders")
        public static class ListCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Parent path", defaultValue = "/content/dam")
            private String path;

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

    @Command(name = "tags", description = "Tag operations", subcommands = {
        TagsCommand.ListCommand.class,
        TagsCommand.CreateCommand.class,
        TagsCommand.DeleteCommand.class
    })
    public static class TagsCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'tags list', 'tags create', or 'tags delete' for operations");
            return 0;
        }

        @Command(name = "list", description = "List tags")
        public static class ListCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Tag namespace/path", defaultValue = "/content/cq:tags")
            private String path;

            @Override
            public Integer call() throws Exception {
                System.out.println("Listing tags in: " + path);
                System.out.println("(Tag listing not fully implemented)");
                return 0;
            }
        }

        @Command(name = "create", description = "Create a tag")
        public static class CreateCommand implements Callable<Integer> {
            @Option(names = {"-n", "--name"}, description = "Tag name (without namespace)", required = true)
            private String name;

            @Option(names = {"-p", "--namespace"}, description = "Tag namespace", defaultValue = "custom")
            private String namespace;

            @Option(names = {"-t", "--title"}, description = "Tag title")
            private String title;

            @Override
            public Integer call() throws Exception {
                String tagId = namespace + ":" + name;
                System.out.println("Creating tag: " + tagId);
                System.out.println("Title: " + (title != null ? title : name));
                System.out.println("(Tag creation not fully implemented)");
                return 0;
            }
        }

        @Command(name = "delete", description = "Delete a tag")
        public static class DeleteCommand implements Callable<Integer> {
            @Option(names = {"-t", "--tag"}, description = "Tag ID (namespace:name)", required = true)
            private String tagId;

            @Override
            public Integer call() throws Exception {
                System.out.println("Deleting tag: " + tagId);
                System.out.println("(Tag deletion not fully implemented)");
                return 0;
            }
        }
    }

    @Command(name = "workflow", description = "Workflow operations", subcommands = {
        WorkflowCommand.ListCommand.class,
        WorkflowCommand.StartCommand.class,
        WorkflowCommand.StatusCommand.class
    })
    public static class WorkflowCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'workflow list', 'workflow start', or 'workflow status' for operations");
            return 0;
        }

        @Command(name = "list", description = "List workflow instances")
        public static class ListCommand implements Callable<Integer> {
            @Option(names = {"-s", "--status"}, description = "Filter by status (RUNNING, COMPLETED, etc.)")
            private String status;

            @Override
            public Integer call() throws Exception {
                System.out.println("Listing workflow instances" + (status != null ? " with status: " + status : ""));
                System.out.println("(Workflow listing not fully implemented)");
                return 0;
            }
        }

        @Command(name = "start", description = "Start a workflow")
        public static class StartCommand implements Callable<Integer> {
            @Option(names = {"-m", "--model"}, description = "Workflow model path", required = true)
            private String model;

            @Option(names = {"-p", "--payload"}, description = "Workflow payload (content path)", required = true)
            private String payload;

            @Override
            public Integer call() throws Exception {
                System.out.println("Starting workflow:");
                System.out.println("  Model: " + model);
                System.out.println("  Payload: " + payload);
                System.out.println("(Workflow start not fully implemented)");
                return 0;
            }
        }

        @Command(name = "status", description = "Get workflow status")
        public static class StatusCommand implements Callable<Integer> {
            @Option(names = {"-i", "--id"}, description = "Workflow instance ID", required = true)
            private String instanceId;

            @Override
            public Integer call() throws Exception {
                System.out.println("Getting status for workflow: " + instanceId);
                System.out.println("(Workflow status not fully implemented)");
                return 0;
            }
        }
    }

    @Command(name = "users", description = "User operations", subcommands = {
        UsersCommand.ListCommand.class,
        UsersCommand.CreateCommand.class,
        UsersCommand.DeleteCommand.class,
        UsersCommand.GroupsCommand.class
    })
    public static class UsersCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'users list', 'users create', 'users delete', or 'users groups' for operations");
            return 0;
        }

        @Command(name = "list", description = "List users")
        public static class ListCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Users path", defaultValue = "/home/users")
            private String path;

            @Override
            public Integer call() throws Exception {
                System.out.println("Listing users in: " + path);
                System.out.println("(User listing not fully implemented)");
                return 0;
            }
        }

        @Command(name = "create", description = "Create a user")
        public static class CreateCommand implements Callable<Integer> {
            @Option(names = {"-u", "--user-id"}, description = "User ID", required = true)
            private String userId;

            @Option(names = {"-p", "--password"}, description = "Password", required = true)
            private String password;

            @Option(names = {"-g", "--groups"}, description = "Groups (comma-separated)")
            private String groups;

            @Override
            public Integer call() throws Exception {
                System.out.println("Creating user: " + userId);
                if (groups != null) {
                    System.out.println("Groups: " + groups);
                }
                System.out.println("(User creation not fully implemented)");
                return 0;
            }
        }

        @Command(name = "delete", description = "Delete a user")
        public static class DeleteCommand implements Callable<Integer> {
            @Option(names = {"-u", "--user-id"}, description = "User ID", required = true)
            private String userId;

            @Override
            public Integer call() throws Exception {
                System.out.println("Deleting user: " + userId);
                System.out.println("(User deletion not fully implemented)");
                return 0;
            }
        }

        @Command(name = "groups", description = "Manage user groups")
        public static class GroupsCommand implements Callable<Integer> {
            @Option(names = {"-u", "--user"}, description = "User ID")
            private String user;

            @Option(names = {"-l", "--list"}, description = "List groups for user")
            private boolean list;

            @Override
            public Integer call() throws Exception {
                if (list && user != null) {
                    System.out.println("Listing groups for user: " + user);
                } else {
                    System.out.println("Use --user <id> --list to list groups for a user");
                }
                System.out.println("(Group management not fully implemented)");
                return 0;
            }
        }
    }

    @Command(name = "replicate", description = "Replication operations", subcommands = {
        ReplicationCommand.PublishCommand.class,
        ReplicationCommand.UnpublishCommand.class,
        ReplicationCommand.StatusCommand.class
    })
    public static class ReplicationCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'replicate publish', 'replicate unpublish', or 'replicate status' for operations");
            return 0;
        }

        @Command(name = "publish", description = "Publish content")
        public static class PublishCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Content path to publish", required = true)
            private String path;

            @Option(names = {"-a", "--agent"}, description = "Replication agent (publish)", defaultValue = "publish")
            private String agent;

            @Override
            public Integer call() throws Exception {
                System.out.println("Publishing content:");
                System.out.println("  Path: " + path);
                System.out.println("  Agent: " + agent);
                System.out.println("(Replication not fully implemented)");
                return 0;
            }
        }

        @Command(name = "unpublish", description = "Unpublish content")
        public static class UnpublishCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Content path to unpublish", required = true)
            private String path;

            @Override
            public Integer call() throws Exception {
                System.out.println("Unpublishing content: " + path);
                System.out.println("(Replication not fully implemented)");
                return 0;
            }
        }

        @Command(name = "status", description = "Check replication status")
        public static class StatusCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Content path", required = true)
            private String path;

            @Override
            public Integer call() throws Exception {
                System.out.println("Checking replication status for: " + path);
                System.out.println("(Status check not fully implemented)");
                return 0;
            }
        }
    }

    @Command(name = "packages", description = "Package operations", subcommands = {
        PackagesCommand.ListCommand.class,
        PackagesCommand.BuildCommand.class,
        PackagesCommand.InstallCommand.class,
        PackagesCommand.UploadCommand.class
    })
    public static class PackagesCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'packages list', 'packages build', 'packages install', or 'packages upload' for operations");
            return 0;
        }

        @Command(name = "list", description = "List packages")
        public static class ListCommand implements Callable<Integer> {
            @Option(names = {"-g", "--group"}, description = "Package group")
            private String group;

            @Override
            public Integer call() throws Exception {
                System.out.println("Listing packages" + (group != null ? " in group: " + group : ""));
                System.out.println("(Package listing not fully implemented)");
                return 0;
            }
        }

        @Command(name = "build", description = "Build a package")
        public static class BuildCommand implements Callable<Integer> {
            @Option(names = {"-n", "--name"}, description = "Package name", required = true)
            private String name;

            @Option(names = {"-g", "--group"}, description = "Package group", defaultValue = "my_packages")
            private String group;

            @Override
            public Integer call() throws Exception {
                System.out.println("Building package: " + group + "/" + name);
                System.out.println("(Package build not fully implemented)");
                return 0;
            }
        }

        @Command(name = "install", description = "Install a package")
        public static class InstallCommand implements Callable<Integer> {
            @Option(names = {"-n", "--name"}, description = "Package name", required = true)
            private String name;

            @Option(names = {"-g", "--group"}, description = "Package group", defaultValue = "my_packages")
            private String group;

            @Override
            public Integer call() throws Exception {
                System.out.println("Installing package: " + group + "/" + name);
                System.out.println("(Package install not fully implemented)");
                return 0;
            }
        }

        @Command(name = "upload", description = "Upload a package")
        public static class UploadCommand implements Callable<Integer> {
            @Option(names = {"-f", "--file"}, description = "Package file path", required = true)
            private String filePath;

            @Option(names = {"-g", "--group"}, description = "Package group", defaultValue = "my_packages")
            private String group;

            @Override
            public Integer call() throws Exception {
                System.out.println("Uploading package: " + filePath);
                System.out.println("Group: " + group);
                System.out.println("(Package upload not fully implemented)");
                return 0;
            }
        }
    }

    @Command(name = "models", description = "Content Fragment Models operations", subcommands = {
        ModelsCommand.ListCommand.class,
        ModelsCommand.CreateCommand.class,
        ModelsCommand.GetCommand.class
    })
    public static class ModelsCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'models list', 'models create', or 'models get' for operations");
            return 0;
        }

        @Command(name = "list", description = "List Content Fragment Models")
        public static class ListCommand implements Callable<Integer> {
            @Option(names = {"-p", "--path"}, description = "Models path", defaultValue = "/conf")
            private String path;

            @Override
            public Integer call() throws Exception {
                System.out.println("Listing Content Fragment Models in: " + path);
                System.out.println("(Model listing not fully implemented)");
                return 0;
            }
        }

        @Command(name = "create", description = "Create a Content Fragment Model")
        public static class CreateCommand implements Callable<Integer> {
            @Option(names = {"-n", "--name"}, description = "Model name", required = true)
            private String name;

            @Option(names = {"-p", "--path"}, description = "Configuration path", required = true)
            private String configPath;

            @Option(names = {"-t", "--title"}, description = "Model title")
            private String title;

            @Override
            public Integer call() throws Exception {
                System.out.println("Creating Content Fragment Model: " + name);
                System.out.println("Config path: " + configPath);
                System.out.println("Title: " + (title != null ? title : name));
                System.out.println("(Model creation not fully implemented)");
                return 0;
            }
        }

        @Command(name = "get", description = "Get a Content Fragment Model")
        public static class GetCommand implements Callable<Integer> {
            @Option(names = {"-n", "--name"}, description = "Model name", required = true)
            private String name;

            @Option(names = {"-p", "--path"}, description = "Configuration path")
            private String configPath;

            @Override
            public Integer call() throws Exception {
                System.out.println("Getting Content Fragment Model: " + name);
                System.out.println("(Model retrieval not fully implemented)");
                return 0;
            }
        }
    }

    @Command(name = "audit", description = "View audit logs", subcommands = {
        AuditCommand.ListCommand.class,
        AuditCommand.ClearCommand.class,
        AuditCommand.CacheCommand.class
    })
    public static class AuditCommand implements Callable<Integer> {
        private static AemApiClient sharedClient;

        public static void setSharedClient(AemApiClient client) {
            sharedClient = client;
        }

        @Override
        public Integer call() throws Exception {
            System.out.println("Use 'audit list', 'audit clear', or 'audit cache' for operations");
            return 0;
        }

        @Command(name = "list", description = "List audit log entries")
        public static class ListCommand implements Callable<Integer> {
            @Option(names = {"-m", "--max"}, description = "Max entries", defaultValue = "50")
            private int max;

            @Override
            public Integer call() throws Exception {
                if (sharedClient == null) {
                    System.out.println("No API client available. Run a command first to initialize.");
                    return 1;
                }
                var auditLog = sharedClient.getAuditLog();
                System.out.println("\nAudit Log (" + auditLog.size() + " entries):");
                int count = 0;
                for (var entry : auditLog.values()) {
                    if (count++ >= max) break;
                    System.out.println("  " + entry);
                }
                return 0;
            }
        }

        @Command(name = "clear", description = "Clear audit log")
        public static class ClearCommand implements Callable<Integer> {
            @Override
            public Integer call() throws Exception {
                System.out.println("Audit log is maintained in memory and clears on restart.");
                System.out.println("(Persistent audit logging not implemented)");
                return 0;
            }
        }

        @Command(name = "cache", description = "API response cache operations")
        public static class CacheCommand implements Callable<Integer> {
            @Option(names = {"--stats"}, description = "Show cache statistics")
            private boolean stats;

            @Option(names = {"--clear"}, description = "Clear the cache")
            private boolean clear;

            @Option(names = {"--enable"}, description = "Enable caching")
            private boolean enable;

            @Option(names = {"--disable"}, description = "Disable caching")
            private boolean disable;

            @Override
            public Integer call() throws Exception {
                if (sharedClient == null) {
                    sharedClient = new AemApiClient();
                }

                if (stats) {
                    System.out.println("\n=== API Cache Stats ===");
                    var cacheStats = sharedClient.getCacheStats();
                    for (var entry : cacheStats.entrySet()) {
                        System.out.println(entry.getKey() + ": " + entry.getValue());
                    }
                    return 0;
                }

                if (clear) {
                    sharedClient.clearCache();
                    System.out.println("API cache cleared.");
                    return 0;
                }

                if (enable) {
                    sharedClient.setCacheEnabled(true);
                    System.out.println("API caching enabled.");
                    return 0;
                }

                if (disable) {
                    sharedClient.setCacheEnabled(false);
                    System.out.println("API caching disabled.");
                    return 0;
                }

                System.out.println("Usage: audit cache --stats|--clear|--enable|--disable");
                return 0;
            }
        }
    }

    @Command(name = "agent", description = "AI-powered AEM assistant")
    public static class AgentCommand implements Callable<Integer> {
        @Option(names = {"-m", "--message"}, description = "Message to send to the agent")
        private String message;

        @Option(names = {"--api-key"}, description = "OpenAI API key (or set OPENAI_API_KEY env)")
        private String apiKey;

        @Option(names = {"--model"}, description = "OpenAI/Ollama model", defaultValue = "gpt-4")
        private String model;

        @Option(names = {"--provider"}, description = "AI provider: openai or ollama", defaultValue = "openai")
        private String provider;

        @Option(names = {"--clear"}, description = "Clear conversation history")
        private boolean clear;

        @Option(names = {"--clear-cache"}, description = "Clear response cache")
        private boolean clearCache;

        @Option(names = {"--no-cache"}, description = "Disable response caching")
        private boolean noCache;

        @Option(names = {"--stats"}, description = "Show memory and cache statistics")
        private boolean stats;

        @Option(names = {"--save-session"}, description = "Save current session to disk")
        private String saveSession;

        @Option(names = {"--load-session"}, description = "Load session from disk")
        private String loadSession;

        @Option(names = {"--list-sessions"}, description = "List saved sessions")
        private boolean listSessions;

        @Option(names = {"--delete-session"}, description = "Delete a saved session")
        private String deleteSession;

        @Option(names = {"--interactive", "-i"}, description = "Enter interactive chat mode")
        private boolean interactive;

        private static AemAgent agent;

        @Override
        public Integer call() throws Exception {
            if (stats || listSessions) {
                try {
                    AgentMemory mem = new AgentMemory();
                    if (listSessions) {
                        System.out.println("\nSaved sessions:");
                        var sessions = (java.util.List<?>) mem.getStats().get("sessions");
                        if (sessions != null && !sessions.isEmpty()) {
                            for (Object s : sessions) {
                                System.out.println("  - " + s);
                            }
                        } else {
                            System.out.println("  (none)");
                        }
                    } else {
                        System.out.println("\n=== Memory & Cache Stats ===");
                        var statsMap = mem.getStats();
                        for (var entry : statsMap.entrySet()) {
                            System.out.println(entry.getKey() + ": " + entry.getValue());
                        }
                    }
                    return 0;
                } catch (IOException e) {
                    System.out.println("Error: Could not load memory: " + e.getMessage());
                    return 1;
                }
            }

            String key = apiKey != null ? apiKey : AemAgent.getApiKey();
            
            String normalizedProvider = provider != null ? provider.toLowerCase().trim() : "openai";
            boolean isOllama = normalizedProvider.equals("ollama");
            
            if (isOllama) {
                key = System.getenv("OLLAMA_API_KEY");
            } else if (key == null || key.isEmpty()) {
                System.out.println("Error: OpenAI API key required.");
                System.out.println("Set OPENAI_API_KEY environment variable or use --api-key");
                return 1;
            }

            ConfigManager config = ConfigManager.getInstance();
            if (config.getActiveEnvironmentUrl() == null) {
                System.out.println("Warning: Not connected to any AEM environment.");
                System.out.println("Run 'connect --env <env> --url <url>' first for better results.");
            }

            if (agent == null || clear || saveSession != null || loadSession != null || listSessions || deleteSession != null || stats) {
                AemAgent.LlmProvider llmProvider = isOllama ? AemAgent.LlmProvider.OLLAMA : AemAgent.LlmProvider.OPENAI;
                agent = new AemAgent(key, model, llmProvider);
            }

            agent.setCacheEnabled(!noCache);

            if (clear) {
                agent.clearHistory();
                System.out.println("Conversation history and memory cleared.");
                return 0;
            }

            if (clearCache) {
                agent.clearCache();
                System.out.println("Cache cleared.");
                return 0;
            }

            if (stats) {
                System.out.println("\n=== Memory & Cache Stats ===");
                var statsMap = agent.getMemoryStats();
                for (var entry : statsMap.entrySet()) {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }
                return 0;
            }

            if (saveSession != null) {
                agent.saveSession(saveSession);
                System.out.println("Session saved: " + saveSession);
                return 0;
            }

            if (loadSession != null) {
                agent.loadSession(loadSession);
                System.out.println("Session loaded: " + loadSession);
                System.out.println("History: " + agent.getMemoryStats().get("history_count") + " messages");
                return 0;
            }

            if (listSessions) {
                System.out.println("\nSaved sessions:");
                for (String s : agent.listSessions()) {
                    System.out.println("  - " + s);
                }
                if (agent.listSessions().isEmpty()) {
                    System.out.println("  (none)");
                }
                return 0;
            }

            if (deleteSession != null) {
                agent.deleteSession(deleteSession);
                System.out.println("Session deleted: " + deleteSession);
                return 0;
            }

            if (interactive) {
                return runInteractive(agent);
            } else if (message != null && !message.isEmpty()) {
                String response = agent.chat(message);
                System.out.println("\n" + response);
                
                if (response.contains("\"action\":")) {
                    System.out.println("\nExecuting action...");
                    String execResponse = agent.executeAction(response);
                    System.out.println(execResponse);
                }
                return 0;
            } else {
                System.out.println("Usage: agent --message \"your request\" or agent --interactive");
                System.out.println("\nMemory options:");
                System.out.println("  --stats              Show memory and cache stats");
                System.out.println("  --clear              Clear conversation history");
                System.out.println("  --clear-cache        Clear response cache");
                System.out.println("  --no-cache           Disable caching");
                System.out.println("  --save-session <n>   Save session to disk");
                System.out.println("  --load-session <n>   Load session from disk");
                System.out.println("  --list-sessions      List saved sessions");
                System.out.println("  --delete-session <n> Delete a session");
                System.out.println("\nExamples:");
                System.out.println("  aem-api agent --message \"list content fragments\"");
                System.out.println("  aem-api agent --message \"upload my logo to /content/dam\"");
                System.out.println("  aem-api agent -i  # Interactive chat mode");
                System.out.println("  aem-api agent --stats");
                return 0;
            }
        }

        private int runInteractive(AemAgent agent) {
            System.out.println("AEM AI Agent (type 'exit' to quit, 'clear' to reset)");
            System.out.println("=================================================");
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            
            while (true) {
                System.out.print("\nYou: ");
                String input = scanner.nextLine().trim();
                
                if (input.equalsIgnoreCase("exit")) {
                    System.out.println("Goodbye!");
                    break;
                }
                
                if (input.equalsIgnoreCase("clear")) {
                    agent.clearHistory();
                    System.out.println("Conversation cleared.");
                    continue;
                }
                
                if (input.isEmpty()) {
                    continue;
                }

                String response = agent.chat(input);
                System.out.println("\nAgent: " + response);
                
                if (response.contains("\"action\":")) {
                    System.out.println("\nExecuting action...");
                    String execResponse = agent.executeAction(response);
                    System.out.println(execResponse);
                }
            }
            return 0;
        }
    }

    @Command(name = "completion", description = "Generate shell completion scripts")
    public static class CompletionCommand implements Callable<Integer> {
        @Option(names = {"--bash"}, description = "Generate bash completion")
        private boolean bash;

        @Option(names = {"--zsh"}, description = "Generate zsh completion")
        private boolean zsh;

        @Option(names = {"--fish"}, description = "Generate fish completion")
        private boolean fish;

        @Option(names = {"--install"}, description = "Install completions to ~/.completion")
        private boolean install;

        @Override
        public Integer call() throws Exception {
            StringBuilder sb = new StringBuilder();
            
            if (bash || (!zsh && !fish)) {
                sb.append(generateBashCompletion());
            }
            
            if (zsh) {
                sb.append(generateZshCompletion());
            }
            
            if (fish) {
                sb.append(generateFishCompletion());
            }
            
            if (install) {
                return installCompletions();
            }
            
            if (sb.length() == 0) {
                System.out.println("Usage: aem-api completion [--bash|--zsh|--fish] [--install]");
                System.out.println("\nOptions:");
                System.out.println("  --bash     Generate bash completion script");
                System.out.println("  --zsh      Generate zsh completion script");
                System.out.println("  --fish     Generate fish completion script");
                System.out.println("  --install  Install completions to ~/.aem-api/completion");
                System.out.println("\nTo use:");
                System.out.println("  aem-api completion --bash > /etc/bash_completion.d/aem-api");
                System.out.println("  source <(aem-api completion --bash)");
                return 0;
            }
            
            System.out.print(sb);
            return 0;
        }

        private int installCompletions() throws Exception {
            java.nio.file.Path home = java.nio.file.Paths.get(System.getProperty("user.home"));
            java.nio.file.Path completionDir = home.resolve(".aem-api/completion");
            java.nio.file.Files.createDirectories(completionDir);
            
            java.nio.file.Files.writeString(completionDir.resolve("aem-api.bash"), generateBashCompletion());
            java.nio.file.Files.writeString(completionDir.resolve("aem-api.zsh"), generateZshCompletion());
            java.nio.file.Files.writeString(completionDir.resolve("aem-api.fish"), generateFishCompletion());
            
            System.out.println("Completions installed to " + completionDir);
            System.out.println("Add to your shell:");
            System.out.println("  Bash: echo 'source " + completionDir.resolve("aem-api.bash") + "' >> ~/.bashrc");
            System.out.println("  Zsh:  echo 'source " + completionDir.resolve("aem-api.zsh") + "' >> ~/.zshrc");
            return 0;
        }

        private String generateBashCompletion() {
            return "#!/bin/bash\n# AEM API Bash Completion\n\n_aem_api() {\n    local cur prev opts\n    COMPREPLY=()\n    cur=\"${COMP_WORDS[COMP_CWORD]}\"\n    prev=\"${COMP_WORDS[COMP_CWORD-1]}\"\n    \n    opts=\"shell connect cf assets sites forms config graphql translation cloudmgr folders tags workflow users replicate packages models audit agent completion help version -v --verbose --debug\"\n    \n    COMPREPLY=( $(compgen -W \"${opts}\" -- ${cur}) )\n    return 0\n}\n\ncomplete -F _aem_api aem-api\n";
        }

        private String generateZshCompletion() {
            return "#compdef aem-api\n# AEM API Zsh Completion\n\n_aem_api() {\n    local -a commands\n    commands=(\n        'shell:Enter interactive shell mode'\n        'connect:Connect to AEM environment'\n        'cf:Content Fragment operations'\n        'assets:Assets operations'\n        'sites:Sites operations'\n        'forms:Forms operations'\n        'config:Configuration management'\n        'graphql:GraphQL operations'\n        'translation:Translation operations'\n        'cloudmgr:Cloud Manager API'\n        'folders:Folder operations'\n        'tags:Tag operations'\n        'workflow:Workflow operations'\n        'users:User operations'\n        'replicate:Replication operations'\n        'packages:Package operations'\n        'models:Content Fragment Models'\n        'audit:View audit logs'\n        'agent:AI-powered AEM assistant'\n        'completion:Generate completions'\n        'help:Show help'\n        'version:Show version'\n    )\n    \n    _describe 'command' commands\n}\n\n_aem_api \"$@\"\n";
        }

        private String generateFishCompletion() {
            return "# AEM API Fish Completion\n\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'shell' -d 'Enter interactive shell mode';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'connect' -d 'Connect to AEM environment';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'cf' -d 'Content Fragment operations';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'assets' -d 'Assets operations';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'sites' -d 'Sites operations';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'forms' -d 'Forms operations';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'config' -d 'Configuration management';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'graphql' -d 'GraphQL operations';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'translation' -d 'Translation operations';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'cloudmgr' -d 'Cloud Manager API';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'folders' -d 'Folder operations';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'tags' -d 'Tag operations';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'workflow' -d 'Workflow operations';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'users' -d 'User operations';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'replicate' -d 'Replication operations';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'packages' -d 'Package operations';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'models' -d 'Content Fragment Models';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'audit' -d 'View audit logs';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'agent' -d 'AI-powered AEM assistant';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'completion' -d 'Generate completions';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'help' -d 'Show help';\n" +
                "complete -c aem-api -f -n '__fish_use_subcommand' -a 'version' -d 'Show version';\n";
        }
    }
}

