package com.aemtools.aem.security;

import java.util.regex.Pattern;

public class InputValidator {

    private static final Pattern SAFE_PATH_PATTERN = Pattern.compile("^[a-zA-Z0-9/_\\-.:=]+$");
    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]+$");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[a-zA-Z0-9./:\\-]+$");

    public static boolean isValidPath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        if (path.contains("..")) {
            return false;
        }
        if (path.contains("\u0000")) {
            return false;
        }
        return true;
    }

    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return SAFE_NAME_PATTERN.matcher(name).matches();
    }

    public static boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return URL_PATTERN.matcher(url).matches();
    }

    public static String sanitizePath(String path) {
        if (path == null) {
            return null;
        }
        return path.replaceAll("\\.\\.", "").replaceAll("\u0000", "");
    }

    public static void validatePath(String path) {
        if (!isValidPath(path)) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
    }

    public static void validateName(String name) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Invalid name: " + name + ". Only alphanumeric, underscore, hyphen allowed.");
        }
    }

    public static void validateUrl(String url) {
        if (!isValidUrl(url)) {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }
    }
}
