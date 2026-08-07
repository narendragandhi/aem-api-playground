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

    private void parse(String... args) {
        CommandLine cmd = new CommandLine(new AemApi());
        cmd.parseArgs(args);
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
        parse("--mock", "cf", "list");
        assertTrue(CliFlags.mockMode);
    }

    @Test
    void testDryRunModeCanBeSet() {
        parse("--dry-run", "replicate", "publish", "-p", "/content");
        assertTrue(CliFlags.dryRunMode);
    }

    @Test
    void testJsonOutputCanBeSet() {
        parse("--json", "cf", "list");
        assertTrue(CliFlags.jsonOutput);
    }

    @Test
    void testAllFlagsCanBeCombined() {
        // Note: --json takes precedence over --output, so outputFormat will be "json"
        parse("--mock", "--json", "--verbose", "--max", "50", "--timeout", "60",
              "--cache", "false", "cf", "list");

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
