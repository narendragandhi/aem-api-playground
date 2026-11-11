package com.aemtools.aem.commands;

import com.aemtools.aem.CliFlags;
import com.aemtools.aem.oak.OakJarResolver;
import com.aemtools.aem.oak.OakOfflineGuard;
import com.aemtools.aem.oak.OakToolRunner;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Offline content migration between Oak repositories backed by the
 * oak-upgrade jar (org.apache.jackrabbit:oak-upgrade). Both source and target
 * must be offline segment stores (or mongo URIs) not in use by any instance.
 * <p>
 * Typical use: migrating content into an upgraded AEM's segment store
 * (the documented offline/rolling upgrade path), with path filters:
 * <pre>
 *   aem-api oak-upgrade --jar oak-upgrade-1.22.jar \
 *     --source old/crx-quickstart/repository/segmentstore \
 *     --target new/crx-quickstart/repository/segmentstore \
 *     --include /content/we-retail --exclude /var
 * </pre>
 */
@Command(name = "oak-upgrade",
        description = "Offline content migration between Oak repositories via oak-upgrade",
        mixinStandardHelpOptions = true)
public class OakUpgradeCommand implements Callable<Integer> {

    @Option(names = {"--jar"},
            description = "Path to the oak-upgrade jar (must match the target's Oak version)")
    String jar;

    @Option(names = {"--source"}, required = true,
            description = "Source repository: segment store path or mongodb:// URI")
    String source;

    @Option(names = {"--target"}, required = true,
            description = "Target repository: segment store path or mongodb:// URI")
    String target;

    @Option(names = {"--include"}, description = "Comma-separated repository paths to include")
    String include;

    @Option(names = {"--exclude"}, description = "Comma-separated repository paths to exclude")
    String exclude;

    @Option(names = {"--merge-excludes"}, description = "Comma-separated paths to merge-exclude")
    String mergeExcludes;

    @Option(names = {"--no-copy-binaries"},
            description = "Skip copying binaries (pass --copy-binaries=false to oak-upgrade)")
    boolean noCopyBinaries;

    @Option(names = {"--version-copy-mode"}, description = "Version copy mode (pass through to oak-upgrade)")
    String versionCopyMode;

    @Option(names = {"--heap"}, description = "JVM max heap for the migration (e.g. 8g)")
    String heap;

    @Option(names = {"--force"}, description = "Run even if a local AEM instance appears to be running")
    boolean force;

    @Option(names = {"--raw"}, description = "Extra arguments passed to oak-upgrade verbatim (quoted string, split on spaces)")
    String raw;

    @Override
    public Integer call() throws Exception {
        if (!OakOfflineGuard.requireStopped(force)) {
            return 1;
        }
        String jarPath;
        try {
            jarPath = OakJarResolver.resolve(jar, "AEM_API_OAK_UPGRADE", "oakUpgradeJar", "oak-upgrade");
        } catch (IllegalStateException e) {
            // --dry-run inspects the command line without the jar on disk
            if (CliFlags.dryRunMode && jar != null && !jar.isBlank()) {
                jarPath = jar.trim();
            } else {
                System.out.println("Error: " + e.getMessage());
                return 1;
            }
        }
        return OakToolRunner.run(buildArgs(jarPath, heap, source, target, include, exclude,
                mergeExcludes, noCopyBinaries, versionCopyMode, raw), false);
    }

    public static List<String> buildArgs(String jarPath, String heap, String source, String target,
            String include, String exclude, String mergeExcludes,
            boolean noCopyBinaries, String versionCopyMode, String raw) {
        List<String> cmd = new ArrayList<>();
        cmd.add("java");
        if (heap != null && !heap.isBlank()) {
            cmd.add("-Xmx" + heap.trim());
        }
        cmd.add("-jar");
        cmd.add(jarPath);
        if (include != null && !include.isBlank()) {
            cmd.add("--include-paths=" + include.trim());
        }
        if (exclude != null && !exclude.isBlank()) {
            cmd.add("--exclude-paths=" + exclude.trim());
        }
        if (mergeExcludes != null && !mergeExcludes.isBlank()) {
            cmd.add("--merge-excludes=" + mergeExcludes.trim());
        }
        if (noCopyBinaries) {
            cmd.add("--copy-binaries=false");
        }
        if (versionCopyMode != null && !versionCopyMode.isBlank()) {
            cmd.add("--version-copy-mode=" + versionCopyMode.trim());
        }
        if (raw != null && !raw.isBlank()) {
            for (String token : raw.trim().split("\\s+")) {
                cmd.add(token);
            }
        }
        cmd.add(source);
        cmd.add(target);
        return cmd;
    }
}
