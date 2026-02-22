package com.example.aem;

import com.example.aem.security.InputValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {

    @Test
    void testValidatePath_Valid() {
        assertTrue(InputValidator.isValidPath("/content/dam/myasset.jpg"));
        assertTrue(InputValidator.isValidPath("/content/my-site/page.html"));
        assertTrue(InputValidator.isValidPath("/libs/granite/"));
    }

    @Test
    void testValidatePath_Null() {
        assertFalse(InputValidator.isValidPath(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"../etc/passwd"})
    void testValidatePath_Invalid(String path) {
        assertFalse(InputValidator.isValidPath(path));
    }

    @Test
    void testIsValidName() {
        assertTrue(InputValidator.isValidName("test-name_123"));
        assertTrue(InputValidator.isValidName("admin"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "test name", "test<script>"})
    void testIsValidName_Invalid(String name) {
        assertFalse(InputValidator.isValidName(name));
    }

    @Test
    void testIsValidUrl() {
        assertTrue(InputValidator.isValidUrl("http://localhost:4502"));
        assertTrue(InputValidator.isValidUrl("https://example.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-a-url", "ftp://example.com"})
    void testIsValidUrl_Invalid(String url) {
        assertFalse(InputValidator.isValidUrl(url));
    }

    @Test
    void testSanitizePath() {
        assertEquals("/content/dam", InputValidator.sanitizePath("/content/dam"));
    }

    @Test
    void testValidatePath_Throws() {
        assertThrows(IllegalArgumentException.class, () -> InputValidator.validatePath(null));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.validatePath("../etc"));
    }

    @Test
    void testValidateName_Throws() {
        assertThrows(IllegalArgumentException.class, () -> InputValidator.validateName(""));
        assertThrows(IllegalArgumentException.class, () -> InputValidator.validateName("test name"));
    }
}
