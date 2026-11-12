package com.aemtools.aem.commands;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.api.JcrApi;
import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command for generic JCR node operations via the Sling POST servlet:
 * read, save, delete, copy, and move arbitrary nodes.
 */
@Command(name = "jcr", description = "Generic JCR node operations (Sling POST servlet)", subcommands = {
    JcrCommand.GetCommand.class,
    JcrCommand.SaveCommand.class,
    JcrCommand.DeleteCommand.class,
    JcrCommand.CopyCommand.class,
    JcrCommand.MoveCommand.class
})
public class JcrCommand implements Callable<Integer> {

    private static AemApiClient sharedClient;

    public static void setSharedClient(AemApiClient client) {
        sharedClient = client;
    }

    private static JcrApi getApi() {
        if (sharedClient == null) {
            sharedClient = new AemApiClient();
        }
        return new JcrApi(sharedClient);
    }

    @Override
    public Integer call() {
        System.out.println("Generic JCR node commands:");
        System.out.println("  jcr get    - Read a node as JSON");
        System.out.println("  jcr save   - Save properties on a node");
        System.out.println("  jcr delete - Delete a node");
        System.out.println("  jcr copy   - Copy a node subtree");
        System.out.println("  jcr move   - Move/rename a node subtree");
        return 0;
    }

    @Command(name = "get", description = "Read a node as JSON")
    public static class GetCommand implements Callable<Integer> {

        @Option(names = {"-p", "--path"}, description = "Node path", required = true)
        private String path;

        @Option(names = {"-d", "--depth"}, description = "Recursion depth (-1 = all levels)", defaultValue = "1")
        private int depth;

        @Override
        public Integer call() throws Exception {
            if (CliFlags.dryRunMode || CliFlags.mockMode) {
                String mode = CliFlags.mockMode ? "MOCK MODE" : "dry-run";
                System.out.println("[" + mode + "] Would GET " + path + "." + depth + ".json");
                return 0;
            }
            try {
                JsonNode node = getApi().getNode(path, depth);
                System.out.println(node.toPrettyString());
                return 0;
            } catch (Exception e) {
                System.err.println("Error reading node: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "save", description = "Save properties on a node")
    public static class SaveCommand implements Callable<Integer> {

        @Option(names = {"-p", "--path"}, description = "Node path", required = true)
        private String path;

        @Option(names = {"-t", "--prop"}, description = "Property as key=value (repeatable); Sling operators "
            + "allowed in the key, e.g. cq:tags@TypeHint=String[]", required = true, arity = "1..")
        private Map<String, String> props = new LinkedHashMap<>();

        @Override
        public Integer call() throws Exception {
            if (CliFlags.dryRunMode || CliFlags.mockMode) {
                String mode = CliFlags.mockMode ? "MOCK MODE" : "dry-run";
                System.out.println("[" + mode + "] Would POST properties to " + path + ": " + props);
                return 0;
            }
            try {
                getApi().saveNode(path, new LinkedHashMap<>(props));
                System.out.println("Saved " + props.size() + " properties on " + path);
                return 0;
            } catch (Exception e) {
                System.err.println("Error saving node: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "delete", description = "Delete a node")
    public static class DeleteCommand implements Callable<Integer> {

        @Option(names = {"-p", "--path"}, description = "Node path", required = true)
        private String path;

        @Option(names = {"--confirm"}, description = "Confirm deletion")
        private boolean confirm;

        @Override
        public Integer call() throws Exception {
            if (CliFlags.dryRunMode || CliFlags.mockMode) {
                String mode = CliFlags.mockMode ? "MOCK MODE" : "dry-run";
                System.out.println("[" + mode + "] Would DELETE " + path);
                return 0;
            }
            if (!confirm) {
                System.out.println("This will permanently delete: " + path);
                System.out.println("Add --confirm to proceed.");
                return 1;
            }
            try {
                getApi().deleteNode(path);
                System.out.println("Deleted node: " + path);
                return 0;
            } catch (Exception e) {
                System.err.println("Error deleting node: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "copy", description = "Copy a node subtree")
    public static class CopyCommand implements Callable<Integer> {

        @Option(names = {"-s", "--source"}, description = "Source node path", required = true)
        private String source;

        @Option(names = {"-d", "--dest"}, description = "Destination path", required = true)
        private String dest;

        @Override
        public Integer call() throws Exception {
            if (CliFlags.dryRunMode || CliFlags.mockMode) {
                String mode = CliFlags.mockMode ? "MOCK MODE" : "dry-run";
                System.out.println("[" + mode + "] Would COPY " + source + " -> " + dest);
                return 0;
            }
            try {
                getApi().copyNode(source, dest);
                System.out.println("Copied " + source + " to " + dest);
                return 0;
            } catch (Exception e) {
                System.err.println("Error copying node: " + e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "move", description = "Move or rename a node subtree")
    public static class MoveCommand implements Callable<Integer> {

        @Option(names = {"-s", "--source"}, description = "Source node path", required = true)
        private String source;

        @Option(names = {"-d", "--dest"}, description = "Destination path", required = true)
        private String dest;

        @Override
        public Integer call() throws Exception {
            if (CliFlags.dryRunMode || CliFlags.mockMode) {
                String mode = CliFlags.mockMode ? "MOCK MODE" : "dry-run";
                System.out.println("[" + mode + "] Would MOVE " + source + " -> " + dest);
                return 0;
            }
            try {
                getApi().moveNode(source, dest);
                System.out.println("Moved " + source + " to " + dest);
                return 0;
            } catch (Exception e) {
                System.err.println("Error moving node: " + e.getMessage());
                return 1;
            }
        }
    }
}
