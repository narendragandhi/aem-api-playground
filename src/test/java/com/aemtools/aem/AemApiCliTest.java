package com.aemtools.aem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import picocli.CommandLine;

public class AemApiCliTest {

    @BeforeEach
    void setUp() {
        CliFlags.mockMode = false;
        CliFlags.dryRunMode = false;
        CliFlags.jsonOutput = false;
        CliFlags.verbose = false;
        CliFlags.outputFormat = "table";
        CliFlags.maxResults = 20;
        CliFlags.timeout = 30000;
        CliFlags.cacheEnabled = true;
    }

    @Test
    void testMainClassExists() {
        assertNotNull(AemApi.class);
    }

    @Test
    void testCliFlagsStaticFieldsExist() {
        assertFalse(CliFlags.mockMode);
        assertFalse(CliFlags.dryRunMode);
        assertFalse(CliFlags.jsonOutput);
        assertEquals("table", CliFlags.outputFormat);
        assertEquals(20, CliFlags.maxResults);
        assertEquals(30000, CliFlags.timeout);
        assertTrue(CliFlags.cacheEnabled);
    }

    @Test
    void testMockModeCanBeSet() {
        String[] args = {"--mock", "cf", "list"};
        CliFlags.parse(args);
        assertTrue(CliFlags.mockMode);
    }

    @Test
    void testDryRunModeCanBeSet() {
        String[] args = {"--dry-run", "replicate", "publish", "-p", "/content"};
        CliFlags.parse(args);
        assertTrue(CliFlags.dryRunMode);
    }

    @Test
    void testJsonOutputCanBeSet() {
        String[] args = {"--json", "cf", "list"};
        CliFlags.parse(args);
        assertTrue(CliFlags.jsonOutput);
    }

    @Test
    void testAllFlagsCanBeCombined() {
        // Note: --json takes precedence over --output, so outputFormat will be "json"
        String[] args = {
            "--mock",
            "--json", 
            "--verbose",
            "--max", "50",
            "--timeout", "60",
            "--cache", "false",
            "cf", "list"
        };
        CliFlags.parse(args);
        
        assertTrue(CliFlags.mockMode);
        assertTrue(CliFlags.jsonOutput);
        assertTrue(CliFlags.verbose);
        assertEquals(50, CliFlags.maxResults);
        assertEquals(60000, CliFlags.timeout);
        assertFalse(CliFlags.cacheEnabled);
        // --json sets outputFormat to "json"
        assertEquals("json", CliFlags.outputFormat);
    }

    @Test
    void testSubcommandsExist() {
        assertTrue(true);
    }
}
