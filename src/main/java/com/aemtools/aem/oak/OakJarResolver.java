package com.aemtools.aem.oak;

import com.aemtools.aem.config.ConfigManager;

import java.io.File;

/**
 * Resolves the path to an Oak tool jar (oak-run or oak-upgrade).
 * <p>
 * Precedence: explicit CLI {@code --jar} value &gt; {@code AEM_API_OAK_RUN} /
 * {@code AEM_API_OAK_UPGRADE} environment variable &gt; saved default
 * ({@code oakRunJar} / {@code oakUpgradeJar} via {@code config defaults}).
 * <p>
 * The jar version must match the Oak version of the target repository; run
 * {@code aem-api oak detect} against the live instance to find that version.
 */
public final class OakJarResolver {

    private OakJarResolver() {
    }

    /**
     * Resolves the tool jar path.
     *
     * @param cliPath    value of the {@code --jar} option (may be null)
     * @param envVarName environment variable to check (e.g. AEM_API_OAK_RUN)
     * @param configKey  ConfigManager default key (e.g. oakRunJar)
     * @param toolLabel  human label used in error messages
     * @return absolute path to an existing jar file
     * @throws IllegalStateException when no path is configured or the file does not exist
     */
    public static String resolve(String cliPath, String envVarName, String configKey, String toolLabel) {
        String path = firstNonBlank(cliPath, System.getenv(envVarName),
                ConfigManager.getInstance().getDefault(configKey, null));
        if (path == null) {
            throw new IllegalStateException("No " + toolLabel + " jar configured. Provide --jar <path>, set "
                    + envVarName + ", or persist it with "
                    + "'config defaults --" + configKey + " <path>'. The jar version must match the Oak version "
                    + "of the target repository; run 'aem-api oak detect' against the live instance to find it.");
        }
        File jarFile = new File(path);
        if (!jarFile.isFile()) {
            throw new IllegalStateException(toolLabel + " jar not found: " + path);
        }
        return jarFile.getAbsolutePath();
    }

    private static String firstNonBlank(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c.trim();
            }
        }
        return null;
    }
}
