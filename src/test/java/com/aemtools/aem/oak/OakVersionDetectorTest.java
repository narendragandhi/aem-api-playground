package com.aemtools.aem.oak;

import com.aemtools.aem.client.AemApiClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("OakVersionDetector")
class OakVersionDetectorTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/system/console/bundles.json", exchange -> {
            byte[] body = """
                {
                  "status": true,
                  "data": [
                    {"id": 0, "symbolicName": "org.apache.jackrabbit.oak-core", "version": "1.22.13"},
                    {"id": 1, "symbolicName": "com.day.crx.crxde", "version": "1.0.0"}
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("Finds the oak-core bundle version in bundles.json")
    void detectsOakCoreVersion() throws IOException {
        AemApiClient client = new AemApiClient().withTarget(baseUrl, "");
        assertEquals("1.22.13", OakVersionDetector.oakCoreVersion(client));
    }

    @Test
    @DisplayName("Returns null when oak-core is absent")
    void returnsNullWhenAbsent() throws IOException {
        server.removeContext("/system/console/bundles.json");
        server.createContext("/system/console/bundles.json", exchange -> {
            byte[] body = """
                {"status": true, "data": [{"id": 0, "symbolicName": "some.other.bundle", "version": "2.0.0"}]}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        AemApiClient client = new AemApiClient().withTarget(baseUrl, "");
        assertNull(OakVersionDetector.oakCoreVersion(client));
    }
}
