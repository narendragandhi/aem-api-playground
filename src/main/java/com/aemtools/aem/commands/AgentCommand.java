package com.aemtools.aem.commands;

import com.aemtools.aem.agent.AemAgent;
import com.aemtools.aem.agent.AgentMemory;
import com.aemtools.aem.config.ConfigManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Command for the AI-powered AEM assistant.
 * Supports both single-message and interactive chat modes,
 * with session management and caching capabilities.
 */
@Command(name = "agent", description = "AI-powered AEM assistant")
public class AgentCommand implements Callable<Integer> {

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

    /**
     * Executes the agent command.
     *
     * @return exit code (0 for success, 1 for failure)
     * @throws Exception if agent operation fails
     */
    @Override
    public Integer call() throws Exception {
        if (stats || listSessions) {
            try {
                AgentMemory mem = new AgentMemory();
                if (listSessions) {
                    System.out.println("\nSaved sessions:");
                    List<?> sessions = (List<?>) mem.getStats().get("sessions");
                    if (sessions != null && !sessions.isEmpty()) {
                        for (Object s : sessions) {
                            System.out.println("  - " + s);
                        }
                    } else {
                        System.out.println("  (none)");
                    }
                } else {
                    System.out.println("\n=== Memory & Cache Stats ===");
                    Map<String, Object> statsMap = mem.getStats();
                    for (Map.Entry<String, Object> entry : statsMap.entrySet()) {
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

        if (agent == null || clear || saveSession != null || loadSession != null
            || listSessions || deleteSession != null || stats) {
            AemAgent.LlmProvider llmProvider = isOllama
                ? AemAgent.LlmProvider.OLLAMA
                : AemAgent.LlmProvider.OPENAI;
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
            Map<String, Object> statsMap = agent.getMemoryStats();
            for (Map.Entry<String, Object> entry : statsMap.entrySet()) {
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
            printUsage();
            return 0;
        }
    }

    /**
     * Runs the interactive chat mode.
     *
     * @param agent the AEM agent instance
     * @return exit code 0
     */
    private int runInteractive(AemAgent agent) {
        System.out.println("AEM AI Agent (type 'exit' to quit, 'clear' to reset)");
        System.out.println("=================================================");
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

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

    /**
     * Prints usage information.
     */
    private void printUsage() {
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
    }
}
