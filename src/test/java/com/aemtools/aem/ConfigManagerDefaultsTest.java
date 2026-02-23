package com.aemtools.aem;

import com.aemtools.aem.config.ConfigManager;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigManagerDefaultsTest {

    @Test
    void testSetDefault() {
        ConfigManager config = ConfigManager.getInstance();
        config.setDefault("testKey", "testValue");
        assertEquals("testValue", config.getDefault("testKey", "default"));
    }

    @Test
    void testGetDefaultWithDefaultValue() {
        ConfigManager config = ConfigManager.getInstance();
        assertEquals("fallback", config.getDefault("nonexistent", "fallback"));
    }

    @Test
    void testGetDefaultInt() {
        ConfigManager config = ConfigManager.getInstance();
        config.setDefault("intKey", "42");
        assertEquals(42, config.getDefaultInt("intKey", 0));
    }

    @Test
    void testGetDefaultIntWithInvalidValue() {
        ConfigManager config = ConfigManager.getInstance();
        config.setDefault("invalid", "notANumber");
        assertEquals(100, config.getDefaultInt("invalid", 100));
    }

    @Test
    void testIsCacheEnabled() {
        ConfigManager config = ConfigManager.getInstance();
        config.setDefault("cache", "true");
        assertTrue(config.isCacheEnabled());
        
        config.setDefault("cache", "false");
        assertFalse(config.isCacheEnabled());
    }

    @Test
    void testDefaultValuesAreSet() {
        ConfigManager config = ConfigManager.getInstance();
        assertEquals("table", config.getDefault("output", ""));
        assertEquals("20", config.getDefault("max", ""));
        assertEquals("30", config.getDefault("timeout", ""));
        assertEquals("true", config.getDefault("cache", ""));
    }
}
