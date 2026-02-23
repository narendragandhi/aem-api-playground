package com.aemtools.aem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CliFlagsTest {

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
    void testParseMockFlag() {
        String[] args = {"--mock", "cf", "list"};
        CliFlags.parse(args);
        assertTrue(CliFlags.mockMode);
        assertFalse(CliFlags.dryRunMode);
        assertFalse(CliFlags.jsonOutput);
    }

    @Test
    void testParseDryRunFlag() {
        String[] args = {"--dry-run", "replicate", "publish", "-p", "/content/test"};
        CliFlags.parse(args);
        assertTrue(CliFlags.dryRunMode);
        assertFalse(CliFlags.mockMode);
    }

    @Test
    void testParseJsonFlag() {
        String[] args = {"--json", "cf", "list"};
        CliFlags.parse(args);
        assertTrue(CliFlags.jsonOutput);
        assertEquals("json", CliFlags.outputFormat);
    }

    @Test
    void testParseOutputFormat() {
        String[] args = {"--output", "raw", "cf", "list"};
        CliFlags.parse(args);
        assertEquals("raw", CliFlags.outputFormat);
    }

    @Test
    void testParseMaxResults() {
        String[] args = {"--max", "100", "cf", "list"};
        CliFlags.parse(args);
        assertEquals(100, CliFlags.maxResults);
    }

    @Test
    void testParseTimeout() {
        String[] args = {"--timeout", "60", "cf", "list"};
        CliFlags.parse(args);
        assertEquals(60000, CliFlags.timeout);
    }

    @Test
    void testParseCacheFlag() {
        String[] args = {"--cache", "false", "cf", "list"};
        CliFlags.parse(args);
        assertFalse(CliFlags.cacheEnabled);
    }

    @Test
    void testParseVerboseFlag() {
        String[] args = {"--verbose", "cf", "list"};
        CliFlags.parse(args);
        assertTrue(CliFlags.verbose);
    }

    @Test
    void testOutputJsonSetsJsonOutput() {
        String[] args = {"--output", "json", "cf", "list"};
        CliFlags.parse(args);
        assertTrue(CliFlags.jsonOutput);
    }

    @Test
    void testDefaultValues() {
        CliFlags.parse(new String[]{});
        assertFalse(CliFlags.mockMode);
        assertFalse(CliFlags.dryRunMode);
        assertFalse(CliFlags.jsonOutput);
        assertEquals("table", CliFlags.outputFormat);
        assertEquals(20, CliFlags.maxResults);
        assertEquals(30000, CliFlags.timeout);
        assertTrue(CliFlags.cacheEnabled);
        assertFalse(CliFlags.verbose);
    }
}
