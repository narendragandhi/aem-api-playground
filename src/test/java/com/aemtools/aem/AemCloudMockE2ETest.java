package com.aemtools.aem;

import com.aemtools.aem.api.PackagesApi;
import com.aemtools.aem.client.AemApiClient;
import com.aemtools.aem.recipes.RecipeEngine;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-end test against a mock AEM as a Cloud Service author. The mock server
 * enforces an IMS Bearer token on every request and implements the PackMgr
 * surface ({@code list.jsp}, {@code service.jsp} upload/build/get) exactly as
 * AEM Cloud exposes it, proving the client's IMS-Bearer auth path and the
 * package/recipe flows work against a cloud-shaped server.
 */
class AemCloudMockE2ETest {

    private static final String TOKEN = "test-cloud-token";

    private HttpServer server;
    private String baseUrl;
    private final List<String> seenAuthHeaders = new ArrayList<>();
    private byte[] backupZip;

    @TempDir
    Path tempDir;

    @BeforeEach
    void startMockCloud() throws IOException {
        backupZip = buildZip("jcr_root/content/mock/page.txt", "mock content");
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopMockCloud() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!path.startsWith("/crx/")) {
            respond(exchange, 404, "not found");
            return;
        }

        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth != null) {
            seenAuthHeaders.add(auth);
        }
        if (auth == null || !auth.equals("Bearer " + TOKEN)) {
            respond(exchange, 401, "{\"error\":\"unauthorized\"}");
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        if (path.equals("/crx/packmgr/list.jsp")) {
            respond(exchange, 200,
                "{\"results\":["
                    + "{\"path\":\"/etc/packages/cloud/vanityurls-components-1.0.6.zip\","
                    + "\"name\":\"vanityurls-components\",\"group\":\"cloud\",\"version\":\"1.0.6\","
                    + "\"size\":17167,\"installed\":true,\"lastBuilt\":\"Tue\",\"description\":\"demo\"},"
                    + "{\"path\":\"/etc/packages/cloud/backup_content.zip\","
                    + "\"name\":\"backup_content\",\"group\":\"cloud\",\"version\":\"\","
                    + "\"size\":1024,\"installed\":false}"
                    + "]}");
            return;
        }

        if (path.equals("/crx/packmgr/service.jsp") && query != null) {
            String cmd = valueOf(query, "cmd");
            String group = valueOf(query, "group");
            String name = valueOf(query, "name");

            if ("upload".equals(cmd)) {
                try (InputStream body = exchange.getRequestBody()) {
                    long uploaded = body.readAllBytes().length;
                    respond(exchange, 200,
                        "<crx><data><package>"
                            + "<group>temporary</group><name>pack_mock_upload</name>"
                            + "<version>1.0</version><size>" + uploaded + "</size>"
                            + "</package></data><status code=\"200\">ok</status></crx>");
                }
                return;
            }

            if ("build".equals(cmd) && "temporary".equals(group)) {
                respond(exchange, 200, "<crx><status code=\"200\">ok</status></crx>");
                return;
            }

            if ("rm".equals(cmd)) {
                respond(exchange, 200, "<crx><status code=\"200\">ok</status></crx>");
                return;
            }

            if ("get".equals(cmd) && "temporary".equals(group)) {
                exchange.getResponseHeaders().add("Content-Type", "application/zip");
                exchange.sendResponseHeaders(200, backupZip.length);
                try (var out = exchange.getResponseBody()) {
                    out.write(backupZip);
                }
                return;
            }
        }

        respond(exchange, 404, "no route");
    }

    private AemApiClient cloudClient() {
        return new AemApiClient().withTarget(baseUrl, "Bearer " + TOKEN);
    }

    private static String valueOf(String query, String key) {
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static byte[] buildZip(String entryName, String content) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(buffer)) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return buffer.toByteArray();
    }

    @Test
    void packagesListSendsBearerTokenAndParses() throws IOException {
        AemApiClient client = cloudClient();
        List<PackagesApi.Package> packages = new PackagesApi(client).list(null);

        assertEquals(2, packages.size());
        PackagesApi.Package first = packages.get(0);
        assertEquals("vanityurls-components", first.getName());
        assertEquals("cloud", first.getGroup());
        assertEquals(17167, first.getSize());
        assertTrue(seenAuthHeaders.contains("Bearer " + TOKEN), "list must send the IMS Bearer token");
    }

    @Test
    void rejectsRequestWithoutToken() {
        AemApiClient client = new AemApiClient().withTarget(baseUrl, "");
        try {
            new PackagesApi(client).list(null);
            fail("expected an IOException for a missing token");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("401"), "unexpected error: " + expected.getMessage());
        }
    }

    @Test
    void contentBackupRecipeRunsEndToEndAgainstMockCloud() throws IOException {
        AemApiClient client = cloudClient();
        RecipeEngine.RecipeResult result = new RecipeEngine(client)
            .contentBackup("/content", tempDir.toString(), "cloud");

        assertTrue(result.success(), "recipe failed: " + result.error());
        assertNotNull(result.steps());
        assertTrue(result.steps().stream().anyMatch(s -> s.startsWith("Backup complete!")),
            "expected a completion step, got: " + result.steps());

        try (var files = Files.list(tempDir)) {
            Path zip = files.findFirst().orElseThrow(() -> new AssertionError("no backup file written"));
            assertTrue(zip.getFileName().toString().endsWith(".zip"));
            assertTrue(Files.size(zip) > 0);

            boolean found = false;
            try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(Files.readAllBytes(zip)))) {
                ZipEntry entry;
                while ((entry = in.getNextEntry()) != null) {
                    if (entry.getName().equals("jcr_root/content/mock/page.txt")) {
                        found = true;
                    }
                }
            }
            assertTrue(found, "downloaded backup must contain the served package content");
        }
        assertTrue(seenAuthHeaders.contains("Bearer " + TOKEN), "recipe must authenticate with Bearer");
    }

    @Test
    void uploadDefinitionRegistersAndBuildsOnMockCloud() throws IOException {
        AemApiClient client = cloudClient();
        PackagesApi api = new PackagesApi(client);

        PackagesApi.Package def = api.uploadDefinition(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<workspaceFilter version=\"1.0\"><filter root=\"/content\"/></workspaceFilter>");

        assertEquals("temporary", def.getGroup());
        assertEquals("pack_mock_upload", def.getName());
        assertTrue(api.build(def.getGroup(), def.getName()), "build must report success");

        Path out = tempDir.resolve("backup.zip");
        assertTrue(api.download(def.getGroup(), def.getName(), out));
        assertTrue(Files.size(out) > 0);
        assertFalse(out.toFile().length() == 0);
    }
}
