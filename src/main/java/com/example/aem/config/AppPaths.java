package com.example.aem.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class AppPaths {

    private static final String APP_NAME = "aem-api";
    private static Path configHome;
    private static Path dataHome;
    private static Path cacheHome;

    static {
        init();
    }

    private static void init() {
        String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        String xdgCacheHome = System.getenv("XDG_CACHE_HOME");

        configHome = (xdgConfigHome != null && !xdgConfigHome.isEmpty())
            ? Paths.get(xdgConfigHome, APP_NAME)
            : Paths.get(System.getProperty("user.home"), ".config", APP_NAME);

        dataHome = (xdgDataHome != null && !xdgDataHome.isEmpty())
            ? Paths.get(xdgDataHome, APP_NAME)
            : Paths.get(System.getProperty("user.home"), ".local", "share", APP_NAME);

        cacheHome = (xdgCacheHome != null && !xdgCacheHome.isEmpty())
            ? Paths.get(xdgCacheHome, APP_NAME)
            : Paths.get(System.getProperty("user.home"), ".cache", APP_NAME);
    }

    public static Path getConfigDir() {
        return configHome;
    }

    public static Path getDataDir() {
        return dataHome;
    }

    public static Path getCacheDir() {
        return cacheHome;
    }

    public static Path getConfigFile() {
        return configHome.resolve("config.yaml");
    }

    public static Path getHistoryFile() {
        return dataHome.resolve("history.txt");
    }

    public static Path getAgentDir() {
        return dataHome.resolve("agent");
    }

    public static Path getAgentHistoryFile() {
        return getAgentDir().resolve("history.json");
    }

    public static Path getAgentCacheFile() {
        return getAgentDir().resolve("cache.json");
    }

    public static Path getAgentSessionsDir() {
        return getAgentDir().resolve("sessions");
    }

    public static Path getMasterKeyFile() {
        return configHome.resolve(".key");
    }

    public static void ensureDirectories() {
        try {
            Files.createDirectories(configHome);
            Files.createDirectories(dataHome);
            Files.createDirectories(cacheHome);
        } catch (java.io.IOException e) {
            System.err.println("Warning: Could not create config directories: " + e.getMessage());
        }
    }

    public static String expandTilde(String path) {
        if (path == null) return null;
        if (path.startsWith("~/")) {
            return path.replaceFirst("^~", System.getProperty("user.home"));
        }
        return path;
    }
}
