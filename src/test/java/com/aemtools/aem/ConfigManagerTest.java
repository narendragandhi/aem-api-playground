package com.aemtools.aem;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.aemtools.aem.config.ConfigManager;

public class ConfigManagerTest {

    @Test
    void testGetInstance() {
        ConfigManager config = ConfigManager.getInstance();
        assertNotNull(config);
    }

    @Test
    void testGetActiveEnvironment() {
        ConfigManager config = ConfigManager.getInstance();
        String active = config.getActiveEnvironment();
        assertNotNull(active);
    }

    @Test
    void testSetActiveEnvironment() {
        ConfigManager config = ConfigManager.getInstance();
        config.setActiveEnvironment("dev");
        assertEquals("dev", config.getActiveEnvironment());
    }
}
