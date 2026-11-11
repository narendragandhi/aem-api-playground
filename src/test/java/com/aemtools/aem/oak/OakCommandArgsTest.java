package com.aemtools.aem.oak;

import com.aemtools.aem.commands.OakCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Oak command argument assembly")
class OakCommandArgsTest {

    private static final String JAR = "/opt/tools/oak-run-1.22.jar";

    @Test
    @DisplayName("check: options before the store path")
    void checkArgs() {
        assertEquals(
            List.of("java", "-jar", JAR, "check", "--fds-path", "/a/datastore", "/path/segmentstore"),
            OakCommand.CheckCommand.buildArgs(JAR, null, "/path/segmentstore", "/a/datastore"));
        assertEquals(
            List.of("java", "-jar", JAR, "check", "/path/segmentstore"),
            OakCommand.CheckCommand.buildArgs(JAR, null, "/path/segmentstore", null));
    }

    @Test
    @DisplayName("heap is rendered as -Xmx")
    void heapRendering() {
        assertEquals(
            List.of("java", "-Xmx4g", "-jar", JAR, "compact", "/path/segmentstore"),
            OakCommand.CompactCommand.buildArgs(JAR, "4g", "/path/segmentstore", null));
    }

    @Test
    @DisplayName("checkpoints: default action is list; rm requires a checkpoint id")
    void checkpointsArgs() {
        assertEquals(
            List.of("java", "-jar", JAR, "checkpoints", "/store", "list"),
            OakCommand.CheckpointsCommand.buildArgs(JAR, null, "/store", null, null));
        assertEquals(
            List.of("java", "-jar", JAR, "checkpoints", "/store", "rm", "abc-123"),
            OakCommand.CheckpointsCommand.buildArgs(JAR, null, "/store", "rm", "abc-123"));
        assertNull(OakCommand.CheckpointsCommand.buildArgs(JAR, null, "/store", "rm", null));
        assertEquals(
            List.of("java", "-jar", JAR, "checkpoints", "/store", "rm-unreferenced"),
            OakCommand.CheckpointsCommand.buildArgs(JAR, null, "/store", "rm-unreferenced", null));
    }

    @Test
    @DisplayName("index: exactly one mode; reindex requires index paths")
    void indexArgs() {
        assertNull(OakCommand.IndexCommand.buildArgs(JAR, null, "/store", null, false, false, false, null));
        assertNull(OakCommand.IndexCommand.buildArgs(JAR, null, "/store", null, true, true, false, null));
        assertNull(OakCommand.IndexCommand.buildArgs(JAR, null, "/store", null, false, false, true, null));
        assertEquals(
            List.of("java", "-jar", JAR, "index", "--reindex", "--index-paths", "/oak:index/cqPageLucene",
                "--fds-path", "/a/datastore", "/store"),
            OakCommand.IndexCommand.buildArgs(JAR, null, "/store", "/a/datastore", false, false, true, "/oak:index/cqPageLucene"));
        assertEquals(
            List.of("java", "-jar", JAR, "index", "--index-info", "/store"),
            OakCommand.IndexCommand.buildArgs(JAR, null, "/store", null, true, false, false, null));
        assertEquals(
            List.of("java", "-jar", JAR, "index", "--index-definitions", "/store"),
            OakCommand.IndexCommand.buildArgs(JAR, null, "/store", null, false, true, false, null));
    }

    @Test
    @DisplayName("backup: store then target")
    void backupArgs() {
        assertEquals(
            List.of("java", "-jar", JAR, "backup", "/store", "/backup/target"),
            OakCommand.BackupCommand.buildArgs(JAR, null, "/store", "/backup/target"));
    }

    @Test
    @DisplayName("export: path defaults to /, blobs optional")
    void exportArgs() {
        assertEquals(
            List.of("java", "-jar", JAR, "export", "-p", "/", "/store", "-o", "/out"),
            OakCommand.ExportCommand.buildArgs(JAR, null, "/store", null, "/out", false));
        assertEquals(
            List.of("java", "-jar", JAR, "export", "-p", "/content/dam", "-b=true", "/store", "-o", "/out"),
            OakCommand.ExportCommand.buildArgs(JAR, null, "/store", "/content/dam", "/out", true));
    }
}
