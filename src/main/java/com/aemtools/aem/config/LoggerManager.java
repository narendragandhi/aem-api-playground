package com.aemtools.aem.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LoggerManager {

    private static LoggerManager instance;
    private final Map<String, String> envVars = new HashMap<>();
    private boolean envLoaded = false;

    private LoggerManager() {
    }

    public static LoggerManager getInstance() {
        if (instance == null) {
            instance = new LoggerManager();
        }
        return instance;
    }

    public void loadEnvFile(String workingDir) {
        if (envLoaded) return;
        
        Path envPath = Paths.get(workingDir, ".env");
        if (Files.exists(envPath)) {
            try (InputStream input = new FileInputStream(envPath.toFile())) {
                Properties props = new Properties();
                props.load(input);
                props.forEach((key, value) -> envVars.put(key.toString(), value.toString()));
                System.out.println("Loaded .env file from: " + envPath);
            } catch (IOException e) {
                System.err.println("Warning: Failed to load .env file: " + e.getMessage());
            }
        }
        envLoaded = true;
    }

    public String getEnv(String key) {
        String envValue = System.getenv(key);
        if (envValue != null) return envValue;
        
        String propKey = key.replace("_", ".");
        String propValue = envVars.get(propKey);
        if (propValue != null) return propValue;
        
        return envVars.get(key);
    }

    public String getEnv(String key, String defaultValue) {
        String value = getEnv(key);
        return value != null ? value : defaultValue;
    }

    public void configureLogging(String logLevel, String logFile) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        ch.qos.logback.classic.Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        
        Level level = parseLevel(logLevel);
        rootLogger.setLevel(level);
        
        context.getLogger("com.aemtools").setLevel(level);
        
        if (logFile != null && !logFile.isEmpty() && !logFile.equals("-")) {
            FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
            fileAppender.setContext(context);
            fileAppender.setName("fileAppender");
            fileAppender.setFile(logFile);
            
            PatternLayout layout = new PatternLayout();
            layout.setContext(context);
            layout.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
            layout.start();
            
            LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
            encoder.setContext(context);
            encoder.setLayout(layout);
            
            fileAppender.setEncoder(encoder);
            fileAppender.start();
            
            rootLogger.addAppender(fileAppender);
        }
    }

    private Level parseLevel(String level) {
        if (level == null) return Level.INFO;
        
        return switch (level.toLowerCase()) {
            case "trace", "silly" -> Level.TRACE;
            case "debug" -> Level.DEBUG;
            case "verbose" -> Level.INFO;
            case "info" -> Level.INFO;
            case "warn", "warning" -> Level.WARN;
            case "error" -> Level.ERROR;
            default -> Level.INFO;
        };
    }

    public Map<String, String> getAllEnv() {
        Map<String, String> all = new HashMap<>(envVars);
        System.getenv().forEach(all::putIfAbsent);
        return all;
    }
}
