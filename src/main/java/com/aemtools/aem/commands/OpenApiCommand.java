package com.aemtools.aem.commands;

import com.aemtools.aem.openapi.OpenApiSupport;
import com.aemtools.aem.openapi.OpenApiSupport.Operation;
import com.fasterxml.jackson.databind.JsonNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Command for exploring Adobe-hosted OpenAPI specs (e.g. the AEMaaCS
 * Launches, MSM, or Assets Author APIs) and generating mock response
 * fixtures from their schemas — the seed of OpenAPI-driven mock generation.
 */
@Command(name = "openapi", description = "Explore OpenAPI specs and generate mock responses", subcommands = {
    OpenApiCommand.ListCommand.class,
    OpenApiCommand.MockCommand.class
})
public class OpenApiCommand implements Callable<Integer> {

    /** Registry bundle base for Adobe-hosted AEMaaCS OpenAPI specs. */
    public static final String ADOBE_REGISTRY_HINT =
        "https://api.redocly.com/registry/bundle/adobe-developers/<SPEC>/stable/openapi.yaml?branch=prod";

    @Override
    public Integer call() {
        System.out.println("OpenAPI commands:");
        System.out.println("  openapi list - List operations in a spec");
        System.out.println("  openapi mock - Generate a mock response from a spec");
        System.out.println("Adobe AEMaaCS specs live under: " + ADOBE_REGISTRY_HINT);
        return 0;
    }

    @Command(name = "list", description = "List operations in an OpenAPI spec")
    public static class ListCommand implements Callable<Integer> {

        @Option(names = {"-s", "--spec"}, description = "Spec URL or local file (YAML or JSON)", required = true)
        private String specSource;

        @Override
        public Integer call() throws Exception {
            try {
                JsonNode spec = OpenApiSupport.loadSpec(specSource);
                System.out.println("\n=== " + spec.path("info").path("title").asText("(untitled)")
                    + " v" + spec.path("info").path("version").asText("?") + " ===");
                for (Operation op : OpenApiSupport.operations(spec)) {
                    System.out.printf("%-8s %-55s %s%n",
                        op.method().toUpperCase(),
                        op.path(),
                        op.summary() != null ? op.summary() : "");
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error loading spec: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "mock", description = "Generate a mock response for one operation")
    public static class MockCommand implements Callable<Integer> {

        @Option(names = {"-s", "--spec"}, description = "Spec URL or local file (YAML or JSON)", required = true)
        private String specSource;

        @Option(names = {"-o", "--operation"}, description = "operationId or \"METHOD /path\"", required = true)
        private String operation;

        @Option(names = {"--status"}, description = "Response status code (default 200, falls back to first 2XX)", defaultValue = "200")
        private int status;

        @Option(names = {"--out"}, description = "Write the mock JSON to a file instead of stdout")
        private String out;

        @Override
        public Integer call() throws Exception {
            try {
                JsonNode spec = OpenApiSupport.loadSpec(specSource);
                Operation op = OpenApiSupport.findOperation(spec, operation);
                if (op == null) {
                    System.err.println("Operation not found: " + operation
                        + " (use 'openapi list' to see available operations)");
                    return 1;
                }
                JsonNode mock = OpenApiSupport.mockResponse(spec, op, status);
                if (mock == null) {
                    System.err.println("No JSON response body defined for "
                        + op + " with status " + status);
                    return 1;
                }
                String json = mock.toPrettyString();
                if (out != null) {
                    Files.writeString(Path.of(out), json + "\n", StandardCharsets.UTF_8);
                    System.out.println("Wrote mock response for " + op + " to " + out);
                } else {
                    System.out.println("\n=== Mock response for " + op + " ===");
                    System.out.println(json);
                }
                return 0;
            } catch (Exception e) {
                System.err.println("Error generating mock: " + e.getMessage());
                return 1;
            }
        }
    }
}
