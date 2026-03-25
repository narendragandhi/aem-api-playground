package com.aemtools.aem.shell;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class PipeProcessor {

    public static class PipeCommand {
        public String command;
        public String[] args;
        public String rawInput;

        public PipeCommand(String command, String[] args, String rawInput) {
            this.command = command;
            this.args = args;
            this.rawInput = rawInput;
        }
    }

    public static class PipeContext {
        public List<Map<String, Object>> previousResults = new ArrayList<>();
        public int exitCode = 0;
        public boolean verbose = false;

        public void clear() {
            previousResults.clear();
            exitCode = 0;
        }
    }

    private static final Pattern CHAIN_PATTERN = Pattern.compile("(.*?)\\s*(\\|\\||&&|;)(.*)");
    private static final Pattern PIPE_PATTERN = Pattern.compile("\\|");

    private final CommandExecutor executor;

    public PipeProcessor(CommandExecutor executor) {
        this.executor = executor;
    }

    public int execute(String input, PipeContext context) {
        input = input.trim();
        if (input.isEmpty()) {
            return 0;
        }

        if (input.contains("|") || input.contains("&&") || input.contains(";") || input.contains("|")) {
            return executeChain(input, context);
        }

        return executeSingle(input, context);
    }

    private int executeChain(String input, PipeContext context) {
        Matcher chainMatcher = CHAIN_PATTERN.matcher(input);

        if (chainMatcher.matches()) {
            String firstPart = chainMatcher.group(1).trim();
            String operator = chainMatcher.group(2);
            String remaining = chainMatcher.group(3).trim();

            int result = executeSingle(firstPart, context);

            if (operator.equals("|")) {
                if (result == 0) {
                    return executeChain(remaining, context);
                }
                return result;
            } else if (operator.equals("||")) {
                if (result != 0) {
                    return executeChain(remaining, context);
                }
                return result;
            } else if (operator.equals("&&")) {
                if (result == 0) {
                    return executeChain(remaining, context);
                }
                return result;
            } else if (operator.equals(";")) {
                return executeChain(remaining, context);
            }
        }

        return executeSingle(input, context);
    }

    private int executeSingle(String input, PipeContext context) {
        String[] parts = parseCommand(input);
        if (parts.length == 0) {
            return 0;
        }

        String cmd = parts[0];
        String[] args = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];

        if (cmd.equals("exit") || cmd.equals("quit")) {
            return -1;
        }

        if (cmd.equals("help")) {
            printHelp();
            return 0;
        }

        if (cmd.equals("pipe")) {
            handlePipeCommand(args, context);
            return 0;
        }

        return executor.execute(cmd, args, context);
    }

    private String[] parseCommand(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if ((c == '"' || c == '\'') && !inQuotes) {
                inQuotes = true;
                quoteChar = c;
            } else if (c == quoteChar && inQuotes) {
                inQuotes = false;
                quoteChar = 0;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens.toArray(new String[0]);
    }

    private void handlePipeCommand(String[] args, PipeContext context) {
        if (args.length == 0) {
            System.out.println("Pipe commands:");
            System.out.println("  pipe list                              List piped data");
            System.out.println("  pipe clear                             Clear piped data");
            System.out.println("  pipe grep <pattern>                   Filter piped data");
            System.out.println("  pipe head [n]                         Show first n items");
            System.out.println("  pipe tail [n]                         Show last n items");
            System.out.println("  pipe json                              Format as JSON");
            System.out.println("  pipe table                             Format as table");
            System.out.println("  pipe count                             Count items");
            System.out.println("  pipe export <file>                    Export to file");
            return;
        }

        String subCmd = args[0];

        switch (subCmd) {
            case "list":
            case "show":
                System.out.println("Piped data (" + context.previousResults.size() + " items):");
                for (int i = 0; i < context.previousResults.size(); i++) {
                    System.out.println("[" + i + "]: " + context.previousResults.get(i));
                }
                break;
            case "clear":
                context.clear();
                System.out.println("Piped data cleared");
                break;
            case "grep":
                if (args.length < 2) {
                    System.out.println("Usage: pipe grep <pattern>");
                    return;
                }
                String pattern = args[1];
                List<Map<String, Object>> filtered = new ArrayList<>();
                for (Map<String, Object> item : context.previousResults) {
                    if (item.toString().toLowerCase().contains(pattern.toLowerCase())) {
                        filtered.add(item);
                    }
                }
                context.previousResults = filtered;
                System.out.println("Filtered to " + filtered.size() + " items");
                break;
            case "filter":
                if (args.length < 2) {
                    System.out.println("Filter options:");
                    System.out.println("  pipe filter --older-than <days>    Filter by age");
                    System.out.println("  pipe filter --modified-after <date> Filter by date");
                    System.out.println("  pipe filter --field <key> <value>  Filter by field value");
                    return;
                }
                handleFilter(args, context);
                break;
            case "head":
                int headCount = args.length > 1 ? Integer.parseInt(args[1]) : 10;
                if (context.previousResults.size() > headCount) {
                    context.previousResults = context.previousResults.subList(0, headCount);
                }
                System.out.println("Showing first " + Math.min(headCount, context.previousResults.size()) + " items");
                break;
            case "tail":
                int tailCount = args.length > 1 ? Integer.parseInt(args[1]) : 10;
                int start = Math.max(0, context.previousResults.size() - tailCount);
                context.previousResults = context.previousResults.subList(start, context.previousResults.size());
                System.out.println("Showing last " + (context.previousResults.size()) + " items");
                break;
            case "count":
                System.out.println("Count: " + context.previousResults.size());
                break;
            case "json":
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(context.previousResults));
                } catch (Exception e) {
                    System.out.println("Error formatting JSON: " + e.getMessage());
                }
                break;
            case "table":
                printAsTable(context.previousResults);
                break;
            case "export":
                if (args.length < 2) {
                    System.out.println("Usage: pipe export <filepath>");
                    return;
                }
                try (PrintWriter writer = new PrintWriter(new FileWriter(args[1], StandardCharsets.UTF_8))) {
                    for (Map<String, Object> item : context.previousResults) {
                        writer.println(item);
                    }
                    System.out.println("Exported to " + args[1]);
                } catch (IOException e) {
                    System.out.println("Error writing file: " + e.getMessage());
                }
                break;
            default:
                System.out.println("Unknown pipe command: " + subCmd);
        }
    }

    private void printAsTable(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            System.out.println("No data to display");
            return;
        }

        Set<String> allKeys = new LinkedHashSet<>();
        for (Map<String, Object> item : data) {
            allKeys.addAll(item.keySet());
        }

        String[] headers = allKeys.toArray(new String[0]);
        int[] widths = new int[headers.length];

        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }

        for (Map<String, Object> item : data) {
            for (int i = 0; i < headers.length; i++) {
                Object value = item.get(headers[i]);
                String str = value != null ? value.toString() : "";
                widths[i] = Math.max(widths[i], str.length());
            }
        }

        StringBuilder separator = new StringBuilder("+");
        for (int w : widths) {
            separator.append("-".repeat(w + 2)).append("+");
        }
        System.out.println(separator);

        StringBuilder headerRow = new StringBuilder("|");
        for (int i = 0; i < headers.length; i++) {
            headerRow.append(" ").append(String.format("%-" + widths[i] + "s", headers[i])).append(" |");
        }
        System.out.println(headerRow);
        System.out.println(separator);

        for (Map<String, Object> item : data) {
            StringBuilder row = new StringBuilder("|");
            for (int i = 0; i < headers.length; i++) {
                Object value = item.get(headers[i]);
                String str = value != null ? value.toString() : "";
                row.append(" ").append(String.format("%-" + widths[i] + "s", str)).append(" |");
            }
            System.out.println(row);
        }
        System.out.println(separator);
    }

    private void handleFilter(String[] args, PipeContext context) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            
            if (arg.equals("--older-than") && i + 1 < args.length) {
                int days = Integer.parseInt(args[++i]);
                long cutoffTime = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L);
                
                for (Map<String, Object> item : context.previousResults) {
                    Object modified = item.get("modified");
                    if (modified != null) {
                        try {
                            long itemTime = parseDateToTimestamp(modified.toString());
                            if (itemTime < cutoffTime) {
                                result.add(item);
                            }
                        } catch (Exception e) {
                            // Skip if can't parse
                        }
                    }
                }
                context.previousResults = result;
                System.out.println("Filtered to " + result.size() + " items older than " + days + " days");
                
            } else if (arg.equals("--modified-after") && i + 1 < args.length) {
                String dateStr = args[++i];
                long cutoffTime;
                try {
                    cutoffTime = parseDateToTimestamp(dateStr);
                } catch (Exception e) {
                    System.out.println("Error parsing date: " + dateStr);
                    continue;
                }
                
                for (Map<String, Object> item : context.previousResults) {
                    Object modified = item.get("modified");
                    if (modified != null) {
                        try {
                            long itemTime = parseDateToTimestamp(modified.toString());
                            if (itemTime > cutoffTime) {
                                result.add(item);
                            }
                        } catch (Exception e) {
                            // Skip
                        }
                    }
                }
                context.previousResults = result;
                System.out.println("Filtered to " + result.size() + " items modified after " + dateStr);
                
            } else if (arg.equals("--field") && i + 2 < args.length) {
                String field = args[++i];
                String value = args[++i];
                
                for (Map<String, Object> item : context.previousResults) {
                    Object fieldValue = item.get(field);
                    if (fieldValue != null && fieldValue.toString().equalsIgnoreCase(value)) {
                        result.add(item);
                    }
                }
                context.previousResults = result;
                System.out.println("Filtered to " + result.size() + " items with " + field + "=" + value);
            }
        }
    }

    private long parseDateToTimestamp(String dateStr) throws Exception {
        try {
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dateStr.replace(" ", "T"));
            return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            try {
                java.time.Instant inst = java.time.Instant.parse(dateStr);
                return inst.toEpochMilli();
            } catch (Exception e2) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    return sdf.parse(dateStr).getTime();
                } catch (Exception e3) {
                    return 0;
                }
            }
        }
    }

    private void printHelp() {
        System.out.println("\n=== Pipe & Chain Commands ===\n");
        System.out.println("Piping:");
        System.out.println("  command1 | command2              Pass output to next command");
        System.out.println("  command1 | grep <pattern>        Filter output");
        System.out.println("  command1 | head [n]             Show first n lines");
        System.out.println("  command1 | tail [n]             Show last n lines");
        System.out.println("  command1 | json                 Format as JSON");
        System.out.println("  command1 | table                Format as table");
        System.out.println("  command1 | export <file>        Save to file");
        System.out.println("\nChaining:");
        System.out.println("  command1 && command2            Run command2 if command1 succeeds");
        System.out.println("  command1 || command2            Run command2 if command1 fails");
        System.out.println("  command1 ; command2            Run command2 after command1");
        System.out.println("\nPipe Management:");
        System.out.println("  pipe list                        Show current piped data");
        System.out.println("  pipe clear                       Clear piped data");
        System.out.println("  pipe count                       Count items");
        System.out.println();
    }

    public interface CommandExecutor {
        int execute(String command, String[] args, PipeContext context);
    }
}
