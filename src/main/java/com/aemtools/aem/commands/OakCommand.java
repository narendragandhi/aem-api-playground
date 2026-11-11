package com.aemtools.aem.commands;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.client.AemApiClient;
import com.aemtools.aem.oak.OakJarResolver;
import com.aemtools.aem.oak.OakOfflineGuard;
import com.aemtools.aem.oak.OakToolRunner;
import com.aemtools.aem.oak.OakVersionDetector;
import com.fasterxml.jackson.databind.JsonNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Offline Oak maintenance commands backed by the oak-run jar
 * (org.apache.jackrabbit:oak-run). All subcommands except {@code detect}
 * operate on the segment store directly and must run while AEM is stopped
 * on the target host, with a jar matching the repository's Oak version.
 */
@Command(name = "oak", description = "Offline Oak maintenance via oak-run (check, compact, checkpoints, index, backup, export, detect)", subcommands = {
    OakCommand.DetectCommand.class,
    OakCommand.CheckCommand.class,
    OakCommand.CompactCommand.class,
    OakCommand.CheckpointsCommand.class,
    OakCommand.IndexCommand.class,
    OakCommand.BackupCommand.class,
    OakCommand.ExportCommand.class
})
public class OakCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("Use 'oak detect', 'oak check', 'oak compact', 'oak checkpoints',"
                + " 'oak index', 'oak backup', or 'oak export'.");
        System.out.println("All except 'detect' are offline segment-store operations: AEM must be"
                + " stopped on the target host and the oak-run jar must match its Oak version.");
        return 0;
    }

    /**
     * Shared options for the offline oak-run subcommands.
     */
    abstract static class OfflineOakBase implements Callable<Integer> {
        @Option(names = {"--jar"},
                description = "Path to the oak-run jar (must match the repo's Oak version; see 'oak detect')")
        String jar;

        @Option(names = {"--store"}, required = true,
                description = "Path to the segment store (e.g. crx-quickstart/repository/segmentstore)")
        String store;

        @Option(names = {"--fds-path"},
                description = "Path to the FileDataStore used by the repository")
        String fdsPath;

        @Option(names = {"--heap"}, description = "JVM max heap for the tool (e.g. 4g)")
        String heap;

        @Option(names = {"--force"},
                description = "Run even if a local AEM instance appears to be running")
        boolean force;

        /**
         * JVM arguments prefix: java [-Xmx..] -jar <oak-run jar>
         */
        static List<String> jvmArgs(String jarPath, String heap) {
            List<String> cmd = new ArrayList<>();
            cmd.add("java");
            if (heap != null && !heap.isBlank()) {
                cmd.add("-Xmx" + heap.trim());
            }
            cmd.add("-jar");
            cmd.add(jarPath);
            return cmd;
        }

        String resolveJar() {
            try {
                return OakJarResolver.resolve(jar, "AEM_API_OAK_RUN", "oakRunJar", "oak-run");
            } catch (IllegalStateException e) {
                // --dry-run inspects the command line without the jar on disk
                if (CliFlags.dryRunMode && jar != null && !jar.isBlank()) {
                    return jar.trim();
                }
                System.out.println("Error: " + e.getMessage());
                return null;
            }
        }

        boolean guard() {
            return OakOfflineGuard.requireStopped(force);
        }
    }

    /**
     * Detects the Oak version of the connected instance so the matching
     * oak-run jar can be picked.
     */
    @Command(name = "detect", description = "Detect the Oak version of the connected AEM instance")
    public static class DetectCommand implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            try {
                String version = OakVersionDetector.oakCoreVersion(new AemApiClient());
                if (version == null) {
                    System.out.println("Could not find the org.apache.jackrabbit.oak-core bundle"
                            + " in /system/console/bundles.json.");
                    return 1;
                }
                System.out.println("Oak core version: " + version);
                System.out.println("Use an oak-run jar built from the same Oak branch, e.g.:");
                System.out.println("  org.apache.jackrabbit:oak-run:" + version);
                System.out.println("Persist it for later runs:");
                System.out.println("  aem-api config defaults --oak-run-jar /path/to/oak-run-<version>.jar");
                return 0;
            } catch (Exception e) {
                System.out.println("Error detecting Oak version: " + e.getMessage());
                System.out.println("Note: /system/console/bundles.json needs a session with console"
                        + " access; connect with admin credentials.");
                return 1;
            }
        }
    }

    /**
     * Runs the oak-run consistency check on a segment store.
     */
    @Command(name = "check", description = "Check the segment store for inconsistencies (offline)")
    public static class CheckCommand extends OfflineOakBase {
        @Override
        public Integer call() throws Exception {
            if (!guard()) {
                return 1;
            }
            String jarPath = resolveJar();
            if (jarPath == null) {
                return 1;
            }
            return OakToolRunner.run(buildArgs(jarPath, heap, store, fdsPath), false);
        }

        public static List<String> buildArgs(String jarPath, String heap, String store, String fds) {
            List<String> cmd = jvmArgs(jarPath, heap);
            cmd.add("check");
            if (fds != null && !fds.isBlank()) {
                cmd.addAll(List.of("--fds-path", fds));
            }
            cmd.add(store);
            return cmd;
        }
    }

    /**
     * Runs segment/tar compaction on a TarMK segment store.
     */
    @Command(name = "compact", description = "Compact a TarMK segment store (offline)")
    public static class CompactCommand extends OfflineOakBase {
        @Override
        public Integer call() throws Exception {
            if (!guard()) {
                return 1;
            }
            String jarPath = resolveJar();
            if (jarPath == null) {
                return 1;
            }
            return OakToolRunner.run(buildArgs(jarPath, heap, store, fdsPath), false);
        }

        public static List<String> buildArgs(String jarPath, String heap, String store, String fds) {
            List<String> cmd = jvmArgs(jarPath, heap);
            cmd.add("compact");
            if (fds != null && !fds.isBlank()) {
                cmd.addAll(List.of("--fds-path", fds));
            }
            cmd.add(store);
            return cmd;
        }
    }

    /**
     * Lists or removes checkpoints on a segment store.
     */
    @Command(name = "checkpoints", description = "List or remove checkpoints (offline)")
    public static class CheckpointsCommand extends OfflineOakBase {
        @Option(names = {"--action"},
                description = "list | rm-all | rm-unreferenced | rm | info (default: list)")
        String action;

        @Option(names = {"--checkpoint"}, description = "Checkpoint id for 'rm' or 'info'")
        String checkpoint;

        @Override
        public Integer call() throws Exception {
            if (!guard()) {
                return 1;
            }
            String jarPath = resolveJar();
            if (jarPath == null) {
                return 1;
            }
            List<String> args = buildArgs(jarPath, heap, store, action, checkpoint);
            if (args == null) {
                return 1;
            }
            return OakToolRunner.run(args, false);
        }

        public static List<String> buildArgs(String jarPath, String heap, String store, String action, String checkpoint) {
            String op = action == null || action.isBlank() ? "list" : action.trim();
            boolean needsId = op.equals("rm") || op.equals("info");
            if (needsId && (checkpoint == null || checkpoint.isBlank())) {
                System.out.println("Error: --checkpoint <id> is required for action '" + op + "'.");
                return null;
            }
            List<String> cmd = jvmArgs(jarPath, heap);
            cmd.add("checkpoints");
            cmd.add(store);
            cmd.add(op);
            if (needsId) {
                cmd.add(checkpoint.trim());
            }
            return cmd;
        }
    }

    /**
     * Inspects or reindexes Lucene indexes via oak-run index.
     */
    @Command(name = "index", description = "Inspect or reindex Lucene indexes (offline)")
    public static class IndexCommand extends OfflineOakBase {
        @Option(names = {"--info"}, description = "Print index stats (--index-info)")
        boolean info;

        @Option(names = {"--definitions"}, description = "Print index definitions (--index-definitions)")
        boolean definitions;

        @Option(names = {"--reindex"}, description = "Reindex the selected index paths")
        boolean reindex;

        @Option(names = {"--index-paths"}, description = "Comma-separated index paths to reindex (e.g. /oak:index/cqPageLucene)")
        String indexPaths;

        @Override
        public Integer call() throws Exception {
            if (!guard()) {
                return 1;
            }
            String jarPath = resolveJar();
            if (jarPath == null) {
                return 1;
            }
            List<String> args = buildArgs(jarPath, heap, store, fdsPath, info, definitions, reindex, indexPaths);
            if (args == null) {
                return 1;
            }
            return OakToolRunner.run(args, false);
        }

        public static List<String> buildArgs(String jarPath, String heap, String store, String fds,
                boolean info, boolean definitions, boolean reindex, String indexPaths) {
            int selected = (info ? 1 : 0) + (definitions ? 1 : 0) + (reindex ? 1 : 0);
            if (selected != 1) {
                System.out.println("Error: pick exactly one of --info, --definitions, or --reindex.");
                return null;
            }
            if (reindex && (indexPaths == null || indexPaths.isBlank())) {
                System.out.println("Error: --index-paths is required with --reindex (e.g. /oak:index/cqPageLucene).");
                return null;
            }
            List<String> cmd = jvmArgs(jarPath, heap);
            cmd.add("index");
            if (info) {
                cmd.add("--index-info");
            } else if (definitions) {
                cmd.add("--index-definitions");
            } else {
                cmd.add("--reindex");
                cmd.add("--index-paths");
                cmd.add(indexPaths.trim());
            }
            if (fds != null && !fds.isBlank()) {
                cmd.addAll(List.of("--fds-path", fds));
            }
            cmd.add(store);
            return cmd;
        }
    }

    /**
     * Backs up a segment store to a target directory.
     */
    @Command(name = "backup", description = "Back up a segment store (offline)")
    public static class BackupCommand extends OfflineOakBase {
        @Option(names = {"--target"}, required = true, description = "Backup target directory or file")
        String target;

        @Override
        public Integer call() throws Exception {
            if (!guard()) {
                return 1;
            }
            String jarPath = resolveJar();
            if (jarPath == null) {
                return 1;
            }
            return OakToolRunner.run(buildArgs(jarPath, heap, store, target), false);
        }

        public static List<String> buildArgs(String jarPath, String heap, String store, String target) {
            List<String> cmd = jvmArgs(jarPath, heap);
            cmd.add("backup");
            cmd.add(store);
            cmd.add(target);
            return cmd;
        }
    }

    /**
     * Exports repository content under a path as JSON (oak-run export).
     */
    @Command(name = "export", description = "Export repository content as JSON (offline)")
    public static class ExportCommand extends OfflineOakBase {
        @Option(names = {"-p", "--path"}, defaultValue = "/", description = "Repository path to export (default: /)")
        String path;

        @Option(names = {"-o", "--out"}, required = true, description = "Output directory")
        String out;

        @Option(names = {"--blobs"}, description = "Also export referenced blobs into the output directory")
        boolean blobs;

        @Override
        public Integer call() throws Exception {
            if (!guard()) {
                return 1;
            }
            String jarPath = resolveJar();
            if (jarPath == null) {
                return 1;
            }
            return OakToolRunner.run(buildArgs(jarPath, heap, store, path, out, blobs), false);
        }

        public static List<String> buildArgs(String jarPath, String heap, String store, String path, String out, boolean blobs) {
            List<String> cmd = jvmArgs(jarPath, heap);
            cmd.add("export");
            cmd.add("-p");
            cmd.add(path == null || path.isBlank() ? "/" : path.trim());
            if (blobs) {
                cmd.add("-b=true");
            }
            cmd.add(store);
            cmd.add("-o");
            cmd.add(out);
            return cmd;
        }
    }
}
