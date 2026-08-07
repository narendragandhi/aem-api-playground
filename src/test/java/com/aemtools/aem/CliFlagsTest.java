package com.aemtools.aem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import picocli.CommandLine;

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

    private void parse(String... args) {
        CommandLine cmd = new CommandLine(new AemApi());
        cmd.parseArgs(args);
    }

    @Test
    void testParseMockFlag() {
        parse("--mock", "cf", "list");
        assertTrue(CliFlags.mockMode);
        assertFalse(CliFlags.dryRunMode);
        assertFalse(CliFlags.jsonOutput);
    }

    @Test
    void testParseDryRunFlag() {
        parse("--dry-run", "replicate", "publish", "-p", "/content/test");
        assertTrue(CliFlags.dryRunMode);
        assertFalse(CliFlags.mockMode);
    }

    @Test
    void testParseJsonFlag() {
        parse("--json", "cf", "list");
        assertTrue(CliFlags.jsonOutput);
        assertEquals("json", CliFlags.outputFormat);
    }

    @Test
    void testParseOutputFormat() {
        parse("--output", "raw", "cf", "list");
        assertEquals("raw", CliFlags.outputFormat);
    }

    @Test
    void testParseMaxResults() {
        parse("--max", "100", "cf", "list");
        assertEquals(100, CliFlags.maxResults);
    }

    @Test
    void testParseTimeout() {
        parse("--timeout", "60", "cf", "list");
        assertEquals(60000, CliFlags.timeout);
    }

    @Test
    void testParseCacheFlag() {
        parse("--cache", "false", "cf", "list");
        assertFalse(CliFlags.cacheEnabled);
    }

    @Test
    void testParseVerboseFlag() {
        parse("--verbose", "cf", "list");
        assertTrue(CliFlags.verbose);
    }

    @Test
    void testOutputJsonSetsJsonOutput() {
        parse("--output", "json", "cf", "list");
        assertTrue(CliFlags.jsonOutput);
    }

    @Test
    void testDefaultValues() {
        parse();
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
