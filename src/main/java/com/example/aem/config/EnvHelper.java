package com.example.aem.config;

import java.util.Map;
import java.util.function.Supplier;

public class EnvHelper {

    private static final Map<String, String> ENV = System.getenv();

    public static String get(String key) {
        return ENV.get(key);
    }

    public static String get(String key, String defaultValue) {
        return ENV.getOrDefault(key, defaultValue);
    }

    public static String getOr(String key, Supplier<String> supplier) {
        String value = ENV.get(key);
        return value != null ? value : supplier.get();
    }

    public static boolean has(String key) {
        return ENV.containsKey(key);
    }

    public static String getApiKey() {
        return get("AEM_API_KEY", get("OPENAI_API_KEY", null));
    }

    public static String getDefaultEnv() {
        return get("AEM_ENV", "dev");
    }

    public static boolean isDebug() {
        return "true".equalsIgnoreCase(get("AEM_DEBUG", "false"));
    }

    public static String getConfigOverride() {
        return get("AEM_CONFIG");
    }

    public static int getTimeout() {
        try {
            return Integer.parseInt(get("AEM_TIMEOUT", "30000"));
        } catch (NumberFormatException e) {
            return 30000;
        }
    }
}
