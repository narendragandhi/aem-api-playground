package com.aemtools.aem;

import com.aemtools.aem.openapi.OpenApiSupport;
import com.aemtools.aem.openapi.OpenApiSupport.Operation;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiSupportTest {

    private static final String SPEC = "src/test/resources/openapi-test-spec.yaml";

    @Test
    void testLoadSpecParsesYamlAndValidatesOpenApiField() throws IOException {
        JsonNode spec = OpenApiSupport.loadSpec(SPEC);
        assertEquals("Test API", spec.path("info").path("title").asText());
        assertEquals("3.0.3", spec.path("openapi").asText());
    }

    @Test
    void testLoadSpecRejectsNonOpenApiFile() throws IOException {
        assertThrows(IOException.class, () -> OpenApiSupport.loadSpec("pom.xml"));
        assertThrows(IOException.class, () -> OpenApiSupport.loadSpec("no-such-file.yaml"));
    }

    @Test
    void testOperationsListsMethodsAndPaths() throws IOException {
        List<Operation> ops = OpenApiSupport.operations(OpenApiSupport.loadSpec(SPEC));
        assertEquals(3, ops.size());
        assertEquals("listLaunches", ops.get(0).operationId());
        assertEquals("GET /launches", ops.get(0).toString());
        assertEquals("getLaunch", ops.get(1).operationId());
        assertEquals("deleteLaunch", ops.get(2).operationId());
    }

    @Test
    void testFindOperationByIdAndByMethodPath() throws IOException {
        JsonNode spec = OpenApiSupport.loadSpec(SPEC);
        assertNotNull(OpenApiSupport.findOperation(spec, "listLaunches"));
        assertNotNull(OpenApiSupport.findOperation(spec, "getLaunch"));
        assertNotNull(OpenApiSupport.findOperation(spec, "GET /launches"));
        assertNull(OpenApiSupport.findOperation(spec, "nope"));
    }

    @Test
    void testMockResponseSynthesizesFromSchema() throws IOException {
        JsonNode spec = OpenApiSupport.loadSpec(SPEC);
        Operation list = OpenApiSupport.findOperation(spec, "listLaunches");
        JsonNode mock = OpenApiSupport.mockResponse(spec, list, 200);

        assertEquals(0, mock.path("total").asInt(-1));
        JsonNode item = mock.path("items").get(0);
        assertNotNull(item);
        assertEquals("spring-promo", item.path("name").asText());
        assertEquals("2026-01-01T00:00:00Z", item.path("created").asText());
        assertEquals("draft", item.path("status").asText());
        assertEquals("00000000-0000-0000-0000-000000000000", item.path("id").asText());
    }

    @Test
    void testMockResponseGuardsRefCycles() throws IOException {
        JsonNode spec = OpenApiSupport.loadSpec(SPEC);
        // Launch.source and Launch.parent both reference Launch — must terminate
        Operation list = OpenApiSupport.findOperation(spec, "listLaunches");
        JsonNode mock = OpenApiSupport.mockResponse(spec, list, 200);
        assertNotNull(mock);
        // Nested source/parent exist but stay within the depth cap
        assertTrue(mock.path("items").get(0).has("source"));
    }

    @Test
    void testMockResponseFallbackAndNull() throws IOException {
        JsonNode spec = OpenApiSupport.loadSpec(SPEC);
        Operation op = OpenApiSupport.findOperation(spec, "getLaunch");
        // A status with no matching response falls back to the first 2XX body
        JsonNode fallback = OpenApiSupport.mockResponse(spec, op, 404);
        assertNotNull(fallback);
        assertEquals("spring-promo", fallback.path("name").asText());
        // A response with no JSON body at all yields null
        Operation delete = OpenApiSupport.findOperation(spec, "deleteLaunch");
        assertNull(OpenApiSupport.mockResponse(spec, delete, 204));
    }

    @Test
    void testSynthTerminatesRefCyclesWithinDepthCap() throws IOException {
        JsonNode spec = OpenApiSupport.loadSpec(SPEC);
        JsonNode cyclic = OpenApiSupport.resolveRef("#/components/schemas/Cyclic", spec);
        JsonNode synth = OpenApiSupport.synthFromSchema(cyclic, spec,
            new java.util.HashSet<>(), 0);

        int depth = 0;
        JsonNode cur = synth;
        while (cur.has("self")) {
            cur = cur.path("self");
            depth++;
            assertTrue(depth <= 6, "cycle guard should stop runaway self-nesting");
        }
        assertTrue(depth >= 1);
    }
}
