package com.aemtools.aem;

/**
 * Runtime store for global CLI flags. Values are populated by picocli through
 * {@link GlobalFlags}; commands and GUI panels read these constants instead of
 * re-parsing the command line.
 */
public final class CliFlags {

    private CliFlags() {
    }

    public static boolean mockMode = false;
    public static boolean dryRunMode = false;
    public static boolean jsonOutput = false;
    public static boolean verbose = false;
    public static String outputFormat = "table";
    public static int maxResults = 20;
    public static int timeout = 30000;
    public static boolean cacheEnabled = true;
}
