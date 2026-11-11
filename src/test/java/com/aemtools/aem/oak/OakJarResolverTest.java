package com.aemtools.aem.oak;

import com.aemtools.aem.config.ConfigManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OakJarResolver precedence")
class OakJarResolverTest {

    private final ConfigManager config = ConfigManager.getInstance();

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        config.setDefault("oakRunJarTest", null);
    }

    private String createJar(String name) throws IOException {
        Path jar = tempDir.resolve(name);
        Files.writeString(jar, "not a real jar, just a placeholder file");
        return jar.toAbsolutePath().toString();
    }

    @Test
    @DisplayName("CLI --jar wins over the saved default")
    void cliPathWins() throws IOException {
        String cliJar = createJar("cli.jar");
        config.setDefault("oakRunJarTest", createJar("saved.jar"));
        assertEquals(cliJar, OakJarResolver.resolve(cliJar, "AEM_API_OAK_RUN_TEST", "oakRunJarTest", "oak-run"));
    }

    @Test
    @DisplayName("Saved default is used when no CLI value is given")
    void savedDefaultUsed() throws IOException {
        String savedJar = createJar("saved.jar");
        config.setDefault("oakRunJarTest", savedJar);
        assertEquals(savedJar, OakJarResolver.resolve(null, "AEM_API_OAK_RUN_TEST", "oakRunJarTest", "oak-run"));
    }

    @Test
    @DisplayName("Fails when no path is configured anywhere")
    void failsWhenUnconfigured() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> OakJarResolver.resolve(null, "AEM_API_OAK_RUN_TEST", "oakRunJarTest", "oak-run"));
        assertTrue(e.getMessage().contains("--jar"));
        assertTrue(e.getMessage().contains("oak detect"));
    }

    @Test
    @DisplayName("Fails when the configured file does not exist")
    void failsWhenFileMissing() {
        config.setDefault("oakRunJarTest", tempDir.resolve("missing.jar").toAbsolutePath().toString());
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> OakJarResolver.resolve(null, "AEM_API_OAK_RUN_TEST", "oakRunJarTest", "oak-run"));
        assertTrue(e.getMessage().contains("not found"));
    }
}
