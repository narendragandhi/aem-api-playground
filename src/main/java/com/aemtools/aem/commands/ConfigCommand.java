package com.aemtools.aem.commands;

import com.aemtools.aem.config.ConfigManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Command for configuration management.
 * Supports viewing configuration, managing environments, and setting defaults.
 */
@Command(name = "config", description = "Configuration management", subcommands = {
    ConfigCommand.ShowCommand.class,
    ConfigCommand.EnvCommand.class,
    ConfigCommand.DefaultsCommand.class
})
public class ConfigCommand implements Callable<Integer> {

    /**
     * Shows usage information when called without subcommand.
     *
     * @return exit code 0
     */
    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'config show' or 'config env' for operations");
        return 0;
    }

    /**
     * Shows the current configuration.
     */
    @Command(name = "show", description = "Show current configuration")
    public static class ShowCommand implements Callable<Integer> {

        /**
         * Displays the current configuration settings.
         *
         * @return exit code 0
         * @throws Exception if configuration cannot be read
         */
        @Override
        public Integer call() throws Exception {
            ConfigManager config = ConfigManager.getInstance();
            config.showConfig();
            return 0;
        }
    }

    /**
     * Manages environment configurations.
     */
    @Command(name = "env", description = "Manage environments")
    public static class EnvCommand implements Callable<Integer> {
        @Option(names = {"-s", "--set"}, description = "Set active environment")
        private String setEnv;

        @Option(names = {"-l", "--list"}, description = "List all environments")
        private boolean list;

        @Option(names = {"--save"}, description = "Save configuration to disk")
        private boolean save;

        /**
         * Executes environment management command.
         *
         * @return exit code 0
         * @throws Exception if operation fails
         */
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

    /**
     * Sets default options for CLI operations.
     */
    @Command(name = "defaults", description = "Set default options")
    public static class DefaultsCommand implements Callable<Integer> {
        @Option(names = {"--output"}, description = "Default output format: table, json, raw")
        private String output;

        @Option(names = {"--max"}, description = "Default max results")
        private Integer max;

        @Option(names = {"--timeout"}, description = "Request timeout in seconds")
        private Integer timeout;

        @Option(names = {"--cache"}, description = "Enable/disable caching: true, false")
        private String cache;

        @Option(names = {"--reset"}, description = "Reset to defaults")
        private boolean reset;

        /**
         * Executes defaults configuration command.
         *
         * @return exit code 0
         * @throws Exception if operation fails
         */
        @Override
        public Integer call() throws Exception {
            ConfigManager config = ConfigManager.getInstance();

            if (reset) {
                config.setDefault("output", "table");
                config.setDefault("max", "20");
                config.setDefault("timeout", "30");
                config.setDefault("cache", "true");
                System.out.println("Defaults reset to factory settings.");
                return 0;
            }

            if (output != null) {
                config.setDefault("output", output);
                System.out.println("Default output set to: " + output);
            }
            if (max != null) {
                config.setDefault("max", max.toString());
                System.out.println("Default max set to: " + max);
            }
            if (timeout != null) {
                config.setDefault("timeout", timeout.toString());
                System.out.println("Default timeout set to: " + timeout + "s");
            }
            if (cache != null) {
                config.setDefault("cache", cache);
                System.out.println("Cache enabled: " + cache);
            }

            config.save();
            System.out.println("\nCurrent defaults:");
            System.out.println("  output: " + config.getDefault("output", "table"));
            System.out.println("  max: " + config.getDefault("max", "20"));
            System.out.println("  timeout: " + config.getDefault("timeout", "30"));
            System.out.println("  cache: " + config.getDefault("cache", "true"));
            return 0;
        }
    }
}
