package com.aemtools.aem.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for the AEM_API_* environment variable overrides that let CI run
 * aem-api without saved credentials (precedence: CLI flags > env > profile).
 */
@DisplayName("ConfigManager env overrides")
class ConfigManagerEnvOverrideTest {

    private static final String ENV = "env-override-test";
    private final Map<String, String> fakeEnv = new HashMap<>();
    private final ConfigManager config = ConfigManager.getInstance();

    @BeforeEach
    void setUp() {
        config.setEnvLookupForTesting(fakeEnv::get);
        config.setActiveEnvironment(ENV);
        config.setEnvironmentUrl(ENV, "http://profile.example:4502");
        config.setBasicAuth(ENV, "cHJvZmlsZTp1c2Vy"); // profile:user
    }

    @AfterEach
    void tearDown() {
        fakeEnv.clear();
        config.setEnvLookupForTesting(System::getenv);
    }

    @Test
    @DisplayName("URL override beats the saved profile")
    void urlOverride() {
        fakeEnv.put("AEM_API_URL", "http://env.example:4502");
        assertEquals("http://env.example:4502", config.getActiveEnvironmentUrl());
    }

    @Test
    @DisplayName("Access token override beats the saved profile")
    void tokenOverride() {
        fakeEnv.put("AEM_API_ACCESS_TOKEN", "ci-token");
        assertEquals("ci-token", config.getActiveAccessToken());
    }

    @Test
    @DisplayName("Basic auth from AEM_API_USER/AEM_API_PASSWORD is base64 encoded")
    void basicAuthOverride() {
        fakeEnv.put("AEM_API_USER", "ciuser");
        fakeEnv.put("AEM_API_PASSWORD", "cipass");
        assertEquals(Base64.getEncoder().encodeToString("ciuser:cipass".getBytes()),
                config.getActiveBasicAuth());
    }

    @Test
    @DisplayName("Basic auth override works with empty password")
    void basicAuthOverrideEmptyPassword() {
        fakeEnv.put("AEM_API_USER", "ciuser");
        assertEquals(Base64.getEncoder().encodeToString("ciuser:".getBytes()),
                config.getActiveBasicAuth());
    }

    @Test
    @DisplayName("AEM_API_ENV selects which saved profile is used")
    void envNameOverride() {
        fakeEnv.put("AEM_API_ENV", ENV);
        config.setActiveEnvironment("other-env");
        config.setEnvironmentUrl("other-env", "http://other.example:4502");
        assertEquals("http://profile.example:4502", config.getActiveEnvironmentUrl());
    }

    @Test
    @DisplayName("Without overrides the saved profile wins")
    void noOverrideFallsBackToProfile() {
        assertEquals("http://profile.example:4502", config.getActiveEnvironmentUrl());
        assertEquals("cHJvZmlsZTp1c2Vy", config.getActiveBasicAuth());
        assertNull(config.getActiveAccessToken());
    }
}
