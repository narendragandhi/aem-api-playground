package com.aemtools.aem.oak;

import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Detects the Oak version of a running AEM instance by reading the
 * {@code /system/console/bundles.json} OSGi console. The detected version
 * determines which oak-run/oak-upgrade jar to use.
 */
public final class OakVersionDetector {

    private OakVersionDetector() {
    }

    /**
     * Finds the {@code org.apache.jackrabbit.oak-core} bundle version.
     *
     * @param client API client pointed at the AEM instance (with credentials)
     * @return the Oak core version, or null when the bundle is not present
     * @throws IOException when the console endpoint is unreachable or returns an error
     */
    public static String oakCoreVersion(AemApiClient client) throws IOException {
        JsonNode root = client.get("/system/console/bundles.json");
        JsonNode data = root.path("data");
        if (!data.isArray()) {
            return null;
        }
        for (JsonNode bundle : data) {
            String symbolicName = bundle.path("symbolicName").asText("");
            if (symbolicName.equals("org.apache.jackrabbit.oak-core")
                    || symbolicName.startsWith("org.apache.jackrabbit.oak-core-")) {
                return bundle.path("version").asText(null);
            }
        }
        return null;
    }
}
