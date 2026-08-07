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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
        response.put("path", "/etc/packages/myg/g/n.zip");
        response.put("version", "2.0");

        when(mockClient.get(eq("/crx/packmgr/g/n.json"))).thenReturn(response);

        Package pkg = packagesApi.get("g", "n");

        assertEquals("g", pkg.getGroup());
        assertEquals("n", pkg.getName());
        assertEquals("2.0", pkg.getVersion());
    }

    @Test
    void testBuildSuccess() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("success", true);
        when(mockClient.post(contains("service.jsp"), any())).thenReturn(response);

        assertTrue(packagesApi.build("g", "n"));
        verify(mockClient).post(eq("/crx/packmgr/service.jsp/g/n"), any());
    }

    @Test
    void testBuildFailure() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("success", false);
        when(mockClient.post(anyString(), any())).thenReturn(response);

        assertFalse(packagesApi.build("g", "n"));
    }

    @Test
    void testInstallSuccess() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("success", true);
        when(mockClient.post(eq("/crx/packmgr/service.jsp/g/n"), any())).thenReturn(response);

        assertTrue(packagesApi.install("g", "n"));
    }

    @Test
    void testUninstallSuccess() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("success", true);
        when(mockClient.post(eq("/crx/packmgr/service.jsp/g/n"), any())).thenReturn(response);

        assertTrue(packagesApi.uninstall("g", "n"));
    }

    @Test
    void testDeleteCallsClient() throws IOException {
        when(mockClient.delete(eq("/crx/packmgr/g/n.json"))).thenReturn(true);

        assertTrue(packagesApi.delete("g", "n"));
    }

    @Test
    void testUploadFromPath() throws IOException {
        Path zip = tempDir.resolve("pkg.zip");
        Files.write(zip, new byte[]{1, 2, 3, 4});

        ObjectNode response = mapper.createObjectNode();
        response.put("success", true);
        ObjectNode pkgNode = mapper.createObjectNode();
        pkgNode.put("path", "/etc/packages/myg/g/pkg.zip");
        response.set("package", pkgNode);

        when(mockClient.upload(contains("cmd=upload"), any(byte[].class), eq("application/zip")))
            .thenReturn(response);

        Package pkg = packagesApi.upload(zip);

        assertEquals("g", pkg.getGroup());
        assertEquals("pkg", pkg.getName());
    }

    @Test
    void testUploadFailureThrows() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("success", false);
        when(mockClient.upload(anyString(), any(byte[].class), anyString())).thenReturn(response);

        assertThrows(IOException.class, () -> packagesApi.upload(new byte[]{1}, "p.zip"));
    }

    @Test
    void testDownloadWritesFile() throws IOException {
        byte[] data = new byte[]{10, 20, 30};
        when(mockClient.download(eq("/crx/packmgr/g/n.zip"))).thenReturn(data);

        Path dest = tempDir.resolve("n.zip");
        assertTrue(packagesApi.download("g", "n", dest));
        assertArrayEquals(data, Files.readAllBytes(dest));
    }

    @Test
    void testRecreatePostsFilter() throws IOException {
        ObjectNode response = mapper.createObjectNode();
        response.put("success", true);
        when(mockClient.post(eq("/crx/packmgr/service.jsp/g/n"), any())).thenReturn(response);

        assertTrue(packagesApi.recreate("g", "n", "<filter root=\"/content\"/>"));
    }
}
