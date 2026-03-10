package com.aemtools.aem;

import com.aemtools.aem.commands.AgentCommand;
import com.aemtools.aem.commands.AssetsCommand;
import com.aemtools.aem.commands.AuditCommand;
import com.aemtools.aem.commands.CloudManagerCommand;
import com.aemtools.aem.commands.CompletionCommand;
import com.aemtools.aem.commands.ConfigCommand;
import com.aemtools.aem.commands.ConnectCommand;
import com.aemtools.aem.commands.ContentFragmentCommand;
import com.aemtools.aem.commands.FoldersCommand;
import com.aemtools.aem.commands.FormsCommand;
import com.aemtools.aem.commands.GraphQLCommand;
import com.aemtools.aem.commands.GuiCommand;
import com.aemtools.aem.commands.ModelsCommand;
import com.aemtools.aem.commands.PackagesCommand;
import com.aemtools.aem.commands.ReplicationCommand;
import com.aemtools.aem.commands.ShellCommand;
import com.aemtools.aem.commands.SitesCommand;
import com.aemtools.aem.commands.TagsCommand;
import com.aemtools.aem.commands.TranslationCommand;
import com.aemtools.aem.commands.UsersCommand;
import com.aemtools.aem.commands.WorkflowCommand;
import com.aemtools.aem.commands.RecipeCommand;
import com.aemtools.aem.config.LoggerManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * AEM API Playground - Interactive CLI for testing Adobe Experience Manager APIs.
 * <p>
 * This is the main entry point for the CLI application. It provides a comprehensive
 * set of commands for interacting with AEM including content fragments, assets,
 * replication, workflows, and more.
 * </p>
 * <p>
 * Usage:
 * <pre>
 *   aem-api [options] &lt;command&gt; [command-options]
 * </pre>
 * </p>
 *
 * @version 1.0.0
 * @author AEM Tools Team
 */
@Command(
    name = "aem-api",
    description = "AEM API Playground - Interactive CLI for testing Adobe Experience Manager APIs",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    sortSynopsis = false,
    subcommands = {
        ShellCommand.class,
        ConnectCommand.class,
        ContentFragmentCommand.class,
        AssetsCommand.class,
        SitesCommand.class,
        FormsCommand.class,
        ConfigCommand.class,
        GraphQLCommand.class,
        TranslationCommand.class,
        CloudManagerCommand.class,
        FoldersCommand.class,
        TagsCommand.class,
        WorkflowCommand.class,
        UsersCommand.class,
        ReplicationCommand.class,
        PackagesCommand.class,
        ModelsCommand.class,
        AuditCommand.class,
        AgentCommand.class,
        CompletionCommand.class,
        GuiCommand.class,
        RecipeCommand.class  // Moved to commands package
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

    @Option(names = {"--mock"}, description = "Use mock data (no AEM connection required)")
    private boolean mock;

    @Option(names = {"--dry-run"}, description = "Show what would happen without making actual changes")
    private boolean dryRun;

    @Option(names = {"--json"}, description = "Output in JSON format")
    private boolean json;

    @Option(names = {"--output"}, description = "Output format: table, json, raw")
    private String output;

    @Option(names = {"--max"}, description = "Max results")
    private int max;

    @Option(names = {"--timeout"}, description = "Request timeout in seconds")
    private int timeout;

    @Option(names = {"--cache"}, description = "Enable/disable cache: true, false")
    private String cache;

    /**
     * Main entry point for the CLI application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        CliFlags.parse(args);

        CommandLine cmd = new CommandLine(new AemApi())
            .setCaseInsensitiveEnumValuesAllowed(true);

        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }

    /**
     * Executes when called without a subcommand.
     * Initializes logging and displays welcome information.
     *
     * @return exit code (0 for success)
     * @throws Exception if initialization fails
     */
    @Override
    public Integer call() throws Exception {
        LoggerManager loggerMgr = LoggerManager.getInstance();

        if (envFile != null && !envFile.isEmpty()) {
            Path p = Paths.get(envFile);
            if (Files.exists(p)) {
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
}
