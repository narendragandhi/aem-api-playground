package com.aemtools.aem.oak;

import com.aemtools.aem.config.ConfigManager;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("OakOfflineGuard")
class OakOfflineGuardTest {

    private HttpServer server;
    private String url;
    private final ConfigManager config = ConfigManager.getInstance();
    private static final String ENV = "oak-guard-test";

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.start();
        url = "http://localhost:" + server.getAddress().getPort();
        config.setEnvironmentUrl(ENV, url);
        config.setActiveEnvironment(ENV);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("Running local AEM refuses the operation without --force")
    void refusesWhenRunning() {
        assertTrue(OakOfflineGuard.isReachable(url));
        assertTrue(OakOfflineGuard.isLocal(url));
        List<String> running = OakOfflineGuard.localRunningInstances();
        assertTrue(running.contains(url), "should detect the running local instance");
        assertFalse(OakOfflineGuard.requireStopped(false), "must refuse without --force");
    }

    @Test
    @DisplayName("--force overrides the check")
    void forceOverrides() {
        assertTrue(OakOfflineGuard.requireStopped(true));
    }

    @Test
    @DisplayName("Unreachable local URL does not trigger the guard")
    void unreachableIsIgnored() {
        config.setActiveEnvironment("oak-guard-none");
        config.setEnvironmentUrl("oak-guard-none", "http://localhost:59999");
        // no other local env has a reachable URL unless another test leaked one
        List<String> running = OakOfflineGuard.localRunningInstances();
        assertFalse(running.contains("http://localhost:59999"));
    }
}
