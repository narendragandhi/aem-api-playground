package com.aemtools.aem;

import picocli.CommandLine.Option;

/**
 * Global options shared by the root command and made available to commands
 * through the {@link CliFlags} runtime store. Declaring these once here (and
 * binding them via picocli) keeps the CLI flag handling in a single place
 * instead of duplicating option declarations on the root command.
 */
public class GlobalFlags {

    @Option(names = {"--mock"}, description = "Use mock data (no AEM connection required)")
    public void setMock(boolean value) {
        CliFlags.mockMode = value;
    }

    @Option(names = {"--dry-run"}, description = "Show what would happen without making actual changes")
    public void setDryRun(boolean value) {
        CliFlags.dryRunMode = value;
    }

    @Option(names = {"--json"}, description = "Output in JSON format")
    public void setJson(boolean value) {
        CliFlags.jsonOutput = value;
        if (value) {
            CliFlags.outputFormat = "json";
        }
    }

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    public void setVerbose(boolean value) {
        CliFlags.verbose = value;
    }

    @Option(names = {"--output"}, description = "Output format: table, json, raw")
    public void setOutput(String value) {
        CliFlags.outputFormat = value;
        if ("json".equalsIgnoreCase(value)) {
            CliFlags.jsonOutput = true;
        }
    }

    @Option(names = {"--max"}, description = "Max results")
    public void setMax(int value) {
        CliFlags.maxResults = value;
    }

    @Option(names = {"--timeout"}, description = "Request timeout in seconds")
    public void setTimeout(int value) {
        CliFlags.timeout = value * 1000;
    }

    @Option(names = {"--cache"}, description = "Enable/disable cache: true, false")
    public void setCache(String value) {
        CliFlags.cacheEnabled = !"false".equalsIgnoreCase(value);
    }
}
