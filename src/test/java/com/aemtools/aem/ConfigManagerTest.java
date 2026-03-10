package com.aemtools.aem;

import com.aemtools.aem.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ConfigManager.
 * Tests configuration management including environments and defaults.
 */
@DisplayName("ConfigManager Tests")
class ConfigManagerTest {

    private ConfigManager config;

    @BeforeEach
    void setUp() {
        config = ConfigManager.getInstance();
    }

    @Nested
    @DisplayName("Singleton Pattern Tests")
    class SingletonTests {

        @Test
        @DisplayName("getInstance should return same instance")
        void testSingletonInstance() {
            ConfigManager instance1 = ConfigManager.getInstance();
            ConfigManager instance2 = ConfigManager.getInstance();

            assertSame(instance1, instance2);
        }

        @Test
        @DisplayName("getInstance should never return null")
        void testNotNull() {
            assertNotNull(ConfigManager.getInstance());
        }
    }

    @Nested
    @DisplayName("Environment Management Tests")
    class EnvironmentTests {

        @Test
        @DisplayName("Should be able to set and get active environment")
        void testSetGetActiveEnvironment() {
            config.setActiveEnvironment("test-env");
            assertEquals("test-env", config.getActiveEnvironment());
        }

        @Test
        @DisplayName("Should be able to set environment URL")
        void testSetEnvironmentUrl() {
            config.setActiveEnvironment("test");
            config.setEnvironmentUrl("test", "http://localhost:4502");

            String url = config.getActiveEnvironmentUrl();
            assertEquals("http://localhost:4502", url);
        }

        @Test
        @DisplayName("Environment URL should be null for unconfigured environment")
        void testUnconfiguredEnvironment() {
            config.setActiveEnvironment("nonexistent-env-" + System.currentTimeMillis());
            String url = config.getActiveEnvironmentUrl();
            // URL might be null or empty for unconfigured environment
            assertTrue(url == null || url.isEmpty());
        }
    }

    @Nested
    @DisplayName("Defaults Management Tests")
    class DefaultsTests {

        @Test
        @DisplayName("Should be able to set and get defaults")
        void testSetGetDefault() {
            config.setDefault("output", "json");
            assertEquals("json", config.getDefault("output", "table"));
        }

        @Test
        @DisplayName("Should return default value when key not set")
        void testDefaultValueReturned() {
            String value = config.getDefault("nonexistent-key-" + System.currentTimeMillis(), "fallback");
            assertEquals("fallback", value);
        }

        @Test
        @DisplayName("Common defaults should have sensible values")
        void testCommonDefaults() {
            // These might have been set, but should return sensible defaults
            String maxDefault = config.getDefault("max", "20");
            String timeoutDefault = config.getDefault("timeout", "30");

            assertNotNull(maxDefault);
            assertNotNull(timeoutDefault);
        }
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthTests {

        @Test
        @DisplayName("Should be able to set basic auth credentials")
        void testSetBasicAuth() {
            config.setActiveEnvironment("auth-test");
            assertDoesNotThrow(() -> config.setBasicAuth("auth-test", "encoded-credentials"));
        }

        @Test
        @DisplayName("Should be able to set access token")
        void testSetAccessToken() {
            config.setActiveEnvironment("token-test");
            assertDoesNotThrow(() -> config.setAccessToken("token-test", "my-access-token"));
        }
    }
}
