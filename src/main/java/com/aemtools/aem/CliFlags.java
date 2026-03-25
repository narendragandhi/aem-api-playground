package com.aemtools.aem;

import java.util.Arrays;

public class CliFlags {
    public static boolean mockMode = false;
    public static boolean dryRunMode = false;
    public static boolean jsonOutput = false;
    public static boolean verbose = false;
    public static String outputFormat = "table";
    public static int maxResults = 20;
    public static int timeout = 30000;
    public static boolean cacheEnabled = true;
    
    public static void parse(String[] args) {
        mockMode = Arrays.asList(args).contains("--mock");
        dryRunMode = Arrays.asList(args).contains("--dry-run");
        jsonOutput = Arrays.asList(args).contains("--json");
        verbose = Arrays.asList(args).contains("--verbose");
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--output") && i + 1 < args.length) {
                outputFormat = args[i + 1];
            }
            if (args[i].equals("--max") && i + 1 < args.length) {
                try { maxResults = Integer.parseInt(args[i + 1]); } catch (Exception e) {}
            }
            if (args[i].equals("--timeout") && i + 1 < args.length) {
                try { timeout = Integer.parseInt(args[i + 1]) * 1000; } catch (Exception e) {}
            }
            if (args[i].equals("--cache") && i + 1 < args.length) {
                cacheEnabled = !"false".equalsIgnoreCase(args[i + 1]);
            }
        }
        
        if (jsonOutput) outputFormat = "json";
        if ("json".equalsIgnoreCase(outputFormat)) jsonOutput = true;
        
        if (verbose) {
            System.err.println("[DEBUG] Args: " + String.join(" ", args));
            System.err.println("[DEBUG] mockMode: " + mockMode + ", jsonOutput: " + jsonOutput + ", outputFormat: " + outputFormat);
        }
    }
}
