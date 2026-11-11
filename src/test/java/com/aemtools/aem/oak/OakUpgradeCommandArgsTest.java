package com.aemtools.aem.oak;

import com.aemtools.aem.commands.OakUpgradeCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OakUpgradeCommand argument assembly")
class OakUpgradeCommandArgsTest {

    private static final String JAR = "/opt/tools/oak-upgrade-1.22.jar";

    @Test
    @DisplayName("Minimal invocation: java -jar <jar> source target")
    void minimalArgs() {
        assertEquals(
            List.of("java", "-jar", JAR, "/old/segmentstore", "/new/segmentstore"),
            OakUpgradeCommand.buildArgs(JAR, null, "/old/segmentstore", "/new/segmentstore",
                null, null, null, false, null, null));
    }

    @Test
    @DisplayName("Path filters and options are passed as oak-upgrade expects")
    void filteredArgs() {
        List<String> args = OakUpgradeCommand.buildArgs(JAR, "8g", "/old/segmentstore", "/new/segmentstore",
            "/content/we-retail,/content/dam", "/var", "/var/garbage", true, "copy", "--merge-excludes=/tmp");
        assertEquals(1, args.stream().filter(a -> a.equals("--include-paths=/content/we-retail,/content/dam")).count());
        assertTrue(args.contains("--exclude-paths=/var"));
        assertTrue(args.contains("--merge-excludes=/var/garbage"));
        assertTrue(args.contains("--copy-binaries=false"));
        assertTrue(args.contains("--version-copy-mode=copy"));
        assertTrue(args.contains("--merge-excludes=/tmp"));
        assertTrue(args.contains("-Xmx8g"));
        // source and target always last
        assertEquals("/old/segmentstore", args.get(args.size() - 2));
        assertEquals("/new/segmentstore", args.get(args.size() - 1));
    }
}
