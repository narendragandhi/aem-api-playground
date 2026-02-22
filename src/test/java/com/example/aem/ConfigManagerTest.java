package com.example.aem;

import com.example.aem.config.ConfigManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void testSetAndGetEnvironmentUrl() {
        ConfigManager config = ConfigManager.getInstance();
        
        config.setEnvironmentUrl("test", "http://localhost:4502");
        config.setActiveEnvironment("test");
        
        assertEquals("http://localhost:4502", config.getActiveEnvironmentUrl());
    }

    @Test
    void testSetAndGetAccessToken() {
        ConfigManager config = ConfigManager.getInstance();
        
        config.setAccessToken("test", "my-token");
        String token = config.getActiveAccessToken();
        
        assertEquals("my-token", token);
    }

    @Test
    void testSetClientId() {
        ConfigManager config = ConfigManager.getInstance();
        
        config.setClientId("test", "my-client-id");
        config.setActiveEnvironment("test");
        
        assertNotNull(config.getActiveEnvironment());
    }

    @Test
    void testOAuthConfig() {
        ConfigManager config = ConfigManager.getInstance();
        
        config.setOAuthConfig("test", 
            "https://ims-na1.adobelogin.com/ims/authorize/v2",
            "https://ims-na1.adobelogin.com/ims/token/v3",
            "test-client-id",
            "test-client-secret",
            "http://localhost:8080/callback");
        
        config.setActiveEnvironment("test");
        
        assertEquals("https://ims-na1.adobelogin.com/ims/authorize/v2", config.getOAuthAuthorizationUrl());
        assertEquals("https://ims-na1.adobelogin.com/ims/token/v3", config.getOAuthTokenUrl());
        assertEquals("test-client-id", config.getOAuthClientId());
        assertEquals("test-client-secret", config.getOAuthClientSecret());
    }

    @Test
    void testListEnvironments() {
        ConfigManager config = ConfigManager.getInstance();
        
        config.setEnvironmentUrl("env1", "http://localhost:4502");
        config.setEnvironmentUrl("env2", "https://prod.example.com");
        
        config.listEnvironments();
    }

    @Test
    void testDebugMode() {
        ConfigManager config = ConfigManager.getInstance();
        
        assertFalse(config.isDebugEnabled());
    }
}
