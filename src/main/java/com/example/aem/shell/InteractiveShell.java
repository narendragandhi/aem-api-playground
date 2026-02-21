package com.example.aem.shell;

import com.example.aem.config.ConfigManager;
import org.jline.reader.*;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class InteractiveShell {

    private final Set<String> commands = new HashSet<>();
    private final Map<String, String> commandDescriptions = new HashMap<>();
    private final ConfigManager configManager;
    private final Map<String, List<String>> commandAliases = new HashMap<>();
    private String historyFile;

    public InteractiveShell(String historyFile) {
        this.configManager = ConfigManager.getInstance();
        this.historyFile = historyFile;
        registerCommands();
    }

    private void registerCommands() {
        commands.add("help");
        commandDescriptions.put("help", "Show available commands");
        
        commands.add("connect");
        commandDescriptions.put("connect", "Connect to an AEM environment");
        
        commands.add("cf");
        commandDescriptions.put("cf", "Content Fragment operations (cf list, cf get, cf create)");
        
        commands.add("assets");
        commandDescriptions.put("assets", "Assets operations (assets list, assets upload, assets delete)");
        
        commands.add("sites");
        commandDescriptions.put("sites", "Sites operations (sites list, sites pages)");
        
        commands.add("forms");
        commandDescriptions.put("forms", "Forms operations (forms list, forms submit)");
        
        commands.add("config");
        commandDescriptions.put("config", "Configuration management (config show, config env)");
        
        commands.add("shell");
        commandDescriptions.put("shell", "Enter interactive shell mode");
        
        commands.add("use");
        commandDescriptions.put("use", "Switch active environment");
        
        commands.add("endpoints");
        commandDescriptions.put("endpoints", "Show available API endpoints");
        
        commands.add("debug");
        commandDescriptions.put("debug", "Toggle debug mode");
        
        commands.add("clear");
        commandDescriptions.put("clear", "Clear the terminal screen");
        
        commands.add("exit");
        commandDescriptions.put("exit", "Exit the shell");
        
        commandAliases.put("exit", Arrays.asList("quit", "bye"));
    }

    public int run() throws Exception {
        Terminal terminal = TerminalBuilder.builder()
                .name("AEM API Playground")
                .jansi(true)
                .build();

        Path histPath = Paths.get(historyFile.replace("~", System.getProperty("user.home")));
        if (histPath.getParent() != null) {
            Files.createDirectories(histPath.getParent());
        }

        DefaultHistory history = new DefaultHistory();
        
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new ArgumentCompleter(
                        new StringsCompleter(commands),
                        NullCompleter.INSTANCE
                ))
                .history(history)
                .build();

        printWelcome(terminal);

        String line;
        while (true) {
            try {
                String env = configManager.getActiveEnvironment();
                String prompt = env != null ? 
                    String.format("\u001B[36maem:%s\u001B[0m> ", env) : 
                    "\u001B[36maem\u001B[0m> ";
                
                line = reader.readLine(prompt);
                
                if (line == null || line.trim().equals("exit") || line.trim().equals("quit")) {
                    break;
                }
                
                if (line.trim().isEmpty()) {
                    continue;
                }

                history.add(line);
                processCommand(line.trim());
                
            } catch (UserInterruptException e) {
                terminal.writer().println("\n(Interrupted)");
            } catch (EndOfFileException e) {
                break;
            }
        }

        terminal.close();
        return 0;
    }

    private void processCommand(String input) {
        String[] parts = input.split("\\s+");
        String cmd = parts[0];
        String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        switch (cmd) {
            case "help":
                showHelp();
                break;
            case "clear":
                System.out.print("\033[H\033[2J");
                System.out.flush();
                break;
            case "use":
                if (args.length > 0) {
                    configManager.setActiveEnvironment(args[0]);
                    System.out.println("Switched to environment: " + args[0]);
                } else {
                    System.out.println("Current environment: " + configManager.getActiveEnvironment());
                }
                break;
            case "endpoints":
                showEndpoints();
                break;
            case "debug":
                configManager.toggleDebug();
                System.out.println("Debug mode: " + (configManager.isDebugEnabled() ? "ON" : "OFF"));
                break;
            case "connect":
                System.out.println("Use: connect --env <name> --url <url> --access-token <token>");
                break;
            case "cf":
            case "assets":
            case "sites":
            case "forms":
            case "config":
                System.out.println("Use: " + cmd + " --help for subcommands");
                break;
            default:
                System.out.println("Unknown command: " + cmd + ". Type 'help' for available commands.");
        }
    }

    private void showHelp() {
        System.out.println("\n\u001B[1mAEM API Playground - Available Commands:\u001B[0m\n");
        System.out.println("\u001B[1mConnection:\u001B[0m");
        System.out.println("  connect --env <name> --url <url> --access-token <token>  Connect to AEM");
        System.out.println("  use <env>                                               Switch environment");
        System.out.println("  config                                                  Show configuration");
        
        System.out.println("\n\u001B[1mContent Fragments:\u001B[0m");
        System.out.println("  cf list [--path <path>]                                List fragments");
        System.out.println("  cf get --path <path>                                    Get fragment");
        System.out.println("  cf create --path <path> --name <name> --model <model>  Create fragment");
        
        System.out.println("\n\u001B[1mAssets:\u001B[0m");
        System.out.println("  assets list [--path <path>]                            List assets");
        System.out.println("  assets upload --file <file> [--path <path>]            Upload asset");
        System.out.println("  assets delete --path <path>                            Delete asset");
        
        System.out.println("\n\u001B[1mSites:\u001B[0m");
        System.out.println("  sites list                                              List sites");
        System.out.println("  sites pages [--path <path>]                            List pages");
        
        System.out.println("\n\u001B[1mForms:\u001B[0m");
        System.out.println("  forms list [--path <path>]                             List forms");
        System.out.println("  forms submit --form <path> --data <json>               Submit form");
        
        System.out.println("\n\u001B[1mUtilities:\u001B[0m");
        System.out.println("  endpoints                                              Show API endpoints");
        System.out.println("  debug                                                  Toggle debug mode");
        System.out.println("  clear                                                  Clear screen");
        System.out.println("  exit                                                   Exit shell");
        System.out.println();
    }

    private void showEndpoints() {
        System.out.println("\n\u001B[1mAEM API Endpoints:\u001B[0m\n");
        System.out.println("\u001B[1mDelivery APIs:\u001B[0m");
        System.out.println("  Content Fragment Delivery:  /api/content/fragments/delivery/...");
        System.out.println("  Dynamic Media Delivery:      /api/assets/dynamicmedia/...");
        
        System.out.println("\n\u001B[1mAuthoring APIs:\u001B[0m");
        System.out.println("  Content Fragments:           /api/sites/content/fragments/...");
        System.out.println("  Assets Author:              /api/assets/author/...");
        System.out.println("  Folders:                    /api/folders/...");
        System.out.println("  Forms Runtime:              /api/forms/...");
        
        System.out.println("\n\u001B[1mManagement APIs:\u001B[0m");
        System.out.println("  Translation:                /api/translation/...");
        System.out.println("  Launches:                   /api/sites/launches/...");
        System.out.println("  MSM:                        /api/sites/msm/...");
        System.out.println();
    }

    private void printWelcome(Terminal terminal) {
        System.out.println("\u001B[1;36m╔═══════════════════════════════════════════════════════════╗\u001B[0m");
        System.out.println("\u001B[1;36m║\u001B[0m   \u001B[1;37mAEM API Playground v1.0.0\u001B[0m                                  \u001B[1;36m║\u001B[0m");
        System.out.println("\u001B[1;36m║\u001B[0m   Interactive CLI for Adobe Experience Manager APIs    \u001B[1;36m║\u001B[0m");
        System.out.println("\u001B[1;36m╚═══════════════════════════════════════════════════════════╝\u001B[0m");
        System.out.println("\nType 'help' for available commands or 'exit' to quit.\n");
    }
}
