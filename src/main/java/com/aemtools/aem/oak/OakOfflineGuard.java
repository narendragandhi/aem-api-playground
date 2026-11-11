package com.aemtools.aem.oak;

import com.aemtools.aem.config.ConfigManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Refuses offline segment-store operations when a local AEM instance appears
 * to be running. Oak tools that open the segment store (check, compact,
 * checkpoints, index, backup, oak-upgrade) must never run against a store
 * that is open by a live AEM; they corrupt it.
 */
public final class OakOfflineGuard {

    private OakOfflineGuard() {
    }

    /**
     * @param force true to override the check (used when the reachable URL is
     *              known to not own the target segment store)
     * @return true when it is safe (or forced) to proceed
     */
    public static boolean requireStopped(boolean force) {
        List<String> running = localRunningInstances();
        if (running.isEmpty()) {
            System.out.println("Note: these tools must run offline with AEM stopped on the target host.");
            return true;
        }
        if (force) {
            System.out.println("Warning: AEM appears to be running at " + running + " (--force given).");
            System.out.println("Make sure it does not own this segment store, or you risk corrupting it.");
            return true;
        }
        System.out.println("Refusing to run: AEM appears to be running at " + running + ".");
        System.out.println("Offline segment-store operations on a live instance can corrupt the repository.");
        System.out.println("Stop AEM first, or pass --force if this check is a false positive.");
        return false;
    }

    /**
     * Lists local (localhost/127.0.0.1) AEM URLs from the saved environments
     * (including the {@code AEM_API_URL} override) that currently respond.
     */
    public static List<String> localRunningInstances() {
        List<String> running = new ArrayList<>();
        for (Map.Entry<String, String> entry : ConfigManager.getInstance().getEnvironmentUrls().entrySet()) {
            addIfRunning(running, entry.getValue());
        }
        addIfRunning(running, ConfigManager.getInstance().getActiveEnvironmentUrl());
        return running;
    }

    private static void addIfRunning(List<String> running, String url) {
        if (url == null || url.isBlank() || "(not set)".equals(url) || running.contains(url)) {
            return;
        }
        if (isLocal(url) && isReachable(url)) {
            running.add(url);
        }
    }

    static boolean isLocal(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null && (host.equals("localhost") || host.equals("127.0.0.1"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static boolean isReachable(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(500);
            connection.setReadTimeout(500);
            connection.setRequestMethod("GET");
            // any HTTP response (even 401/404) means the instance is up
            return connection.getResponseCode() > 0;
        } catch (IOException | IllegalArgumentException e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
