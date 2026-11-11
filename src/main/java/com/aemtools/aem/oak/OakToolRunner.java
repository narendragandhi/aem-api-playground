package com.aemtools.aem.oak;

import com.aemtools.aem.CliFlags;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Executes oak-run / oak-upgrade tool jars as local processes.
 * <p>
 * CLI commands use {@link #run(List, boolean)} which honors the global
 * {@code --dry-run} flag (prints the command line instead of executing) and
 * inherits the terminal's IO. The MCP server uses {@link #runCaptured(List)}
 * to collect the tool output into a result object.
 */
public final class OakToolRunner {

    /**
     * Result of a captured (non-interactive) tool run.
     */
    public record OakResult(int exitCode, String output) {
    }

    private OakToolRunner() {
    }

    /**
     * Runs the tool, honoring {@code --dry-run} and {@code --verbose}.
     *
     * @param command full command line including the java executable
     * @param dryRun  true to only print the command
     * @return process exit code (0 on dry-run)
     */
    public static int run(List<String> command, boolean dryRun) {
        String printable = String.join(" ", command);
        if (dryRun || CliFlags.dryRunMode) {
            System.out.println("[dry-run] Would run:");
            System.out.println("  " + printable);
            return 0;
        }
        if (CliFlags.verbose) {
            System.out.println("$ " + printable);
        }
        try {
            Process process = new ProcessBuilder(command).inheritIO().start();
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Interrupted while running: " + printable);
            return 1;
        } catch (Exception e) {
            System.out.println("Error running oak tool: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Runs the tool capturing combined stdout/stderr, for the MCP server.
     *
     * @param command full command line including the java executable
     * @result exit code and output of the run
     */
    public static OakResult runCaptured(List<String> command) {
        String printable = String.join(" ", command);
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }
            int exitCode = process.waitFor();
            return new OakResult(exitCode, output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new OakResult(1, "Interrupted while running: " + printable);
        } catch (Exception e) {
            return new OakResult(1, "Error running oak tool: " + e.getMessage());
        }
    }
}
