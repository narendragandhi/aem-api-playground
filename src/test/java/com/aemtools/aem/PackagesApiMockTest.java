package com.aemtools.aem;

import com.aemtools.aem.api.PackagesApi;
import com.aemtools.aem.api.PackagesApi.Package;
import com.aemtools.aem.client.AemApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class PackagesApiMockTest {

    @Mock
    private AemApiClient mockClient;

    private PackagesApi packagesApi;
    private ObjectMapper mapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        when(mockClient.getObjectMapper()).thenReturn(mapper);
        packagesApi = new PackagesApi(mockClient);
    }

    @Test
    void testListParsesResults() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode results = mapper.createArrayNode();
        ObjectNode pkg = mapper.createObjectNode();
        pkg.put("path", "/etc/packages/myg/mygroup/mypkg.zip");
        pkg.put("version", "1.0.0");
        pkg.put("size", 1234L);
        results.add(pkg);
        response.set("results", results);

        when(mockClient.get(contains("packmgr/list.jsp"))).thenReturn(response);

        List<Package> packages = packagesApi.list("mygroup");

        assertEquals(1, packages.size());
        assertEquals("mygroup", packages.get(0).getGroup());
        assertEquals("mypkg", packages.get(0).getName());
        assertEquals("1.0.0", packages.get(0).getVersion());
    }

    @Test
    void testListWithoutGroup() throws IOException {
        when(mockClient.get(contains("packmgr/list.jsp"))).thenReturn(mapper.createObjectNode());

        List<Package> packages = packagesApi.list(null);

        assertTrue(packages.isEmpty());
        verify(mockClient).get(eq("/crx/packmgr/list.jsp"));
    }

    @Test
    void testGetParsesPackage() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        ArrayNode results = mapper.createArrayNode();
        ObjectNode pkgNode = mapper.createObjectNode();
        pkgNode.put("path", "/etc/packages/g/n.zip");
        pkgNode.put("version", "2.0");
        results.add(pkgNode);
        response.set("results", results);

        when(mockClient.get(contains("list.jsp"))).thenReturn(response);

        Package pkg = packagesApi.get("g", "n");

        assertEquals("g", pkg.getGroup());
        assertEquals("n", pkg.getName());
        assertEquals("2.0", pkg.getVersion());
    }

    @Test
    void testBuildSuccess() throws IOException {
        when(mockClient.postRaw(contains("cmd=build")))
            .thenReturn(new AemApiClient.RawResponse(200, "<crx><status code=\"200\">ok</status></crx>"));

        assertTrue(packagesApi.build("g", "n"));
        verify(mockClient).postRaw(eq("/crx/packmgr/service.jsp?cmd=build&name=n&group=g"));
    }

    @Test
    void testBuildFailure() throws IOException {
        when(mockClient.postRaw(anyString()))
            .thenReturn(new AemApiClient.RawResponse(500, "<crx><status code=\"500\">boom</status></crx>"));

        assertFalse(packagesApi.build("g", "n"));
    }

    @Test
    void testInstallSuccess() throws IOException {
        when(mockClient.postRaw(contains("cmd=inst")))
            .thenReturn(new AemApiClient.RawResponse(200, "<crx><status code=\"200\">ok</status></crx>"));

        assertTrue(packagesApi.install("g", "n"));
    }

    @Test
    void testUninstallSuccess() throws IOException {
        when(mockClient.postRaw(contains("cmd=uninst")))
            .thenReturn(new AemApiClient.RawResponse(200, "<crx><status code=\"200\">ok</status></crx>"));

        assertTrue(packagesApi.uninstall("g", "n"));
    }

    @Test
    void testDeleteCallsClient() throws IOException {
        when(mockClient.postRaw(contains("cmd=rm")))
            .thenReturn(new AemApiClient.RawResponse(200, "<crx><status code=\"200\">ok</status></crx>"));

        assertTrue(packagesApi.delete("g", "n"));
    }

    @Test
    void testUploadFromPath() throws IOException {
        Path zip = tempDir.resolve("pkg.zip");
        Files.write(zip, new byte[]{1, 2, 3, 4});

        String xml = "<crx><response><data><package>"
            + "<group>temporary</group><name>pack_abc</name><version>1.0</version><size>123</size>"
            + "</package></data><status code=\"200\">ok</status></response></crx>";

        when(mockClient.uploadMultipart(contains("cmd=upload"), eq("file"), anyString(), any(byte[].class)))
            .thenReturn(xml);

        Package pkg = packagesApi.upload(zip);

        assertEquals("temporary", pkg.getGroup());
        assertEquals("pack_abc", pkg.getName());
        assertEquals("1.0", pkg.getVersion());
    }

    @Test
    void testUploadFailureThrows() throws IOException {
        when(mockClient.uploadMultipart(anyString(), anyString(), anyString(), any(byte[].class)))
            .thenReturn("<crx><status code=\"500\">Given archive is not a content package</status></crx>");

        assertThrows(IOException.class, () -> packagesApi.upload(new byte[]{1}, "p.zip"));
    }

    @Test
    void testDownloadWritesFile() throws IOException {
        byte[] data = new byte[]{10, 20, 30};
        when(mockClient.download(eq("/crx/packmgr/service.jsp?cmd=get&name=n&group=g"))).thenReturn(data);

        Path dest = tempDir.resolve("n.zip");
        assertTrue(packagesApi.download("g", "n", dest));
        assertArrayEquals(data, Files.readAllBytes(dest));
    }

    @Test
    void testUploadDefinitionBuildsSkeletonZip() throws IOException {
        String uploadResponse =
            "<crx><data><package><group>temporary</group>"
                + "<name>pack_abc123</name><version>1.0</version><size>42</size>"
                + "</package></data><status code=\"200\">ok</status></crx>";
        ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        when(mockClient.uploadMultipart(anyString(), anyString(), anyString(), captor.capture()))
            .thenReturn(uploadResponse);

        PackagesApi.Package pkg = packagesApi.uploadDefinition(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<workspaceFilter version=\"1.0\"><filter root=\"/content\"/></workspaceFilter>");

        assertEquals("temporary", pkg.getGroup());
        assertEquals("pack_abc123", pkg.getName());
        assertEquals("1.0", pkg.getVersion());

        byte[] zipData = captor.getValue();
        Map<String, String> entries = new java.util.HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertTrue(entries.containsKey("META-INF/vault/filter.xml"));
        assertTrue(entries.get("META-INF/vault/filter.xml").contains("<filter root=\"/content\"/>"));
        assertTrue(entries.containsKey("jcr_root/.content.xml"));
    }
}
