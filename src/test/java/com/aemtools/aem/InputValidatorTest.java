package com.aemtools.aem;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.aemtools.aem.security.InputValidator;

public class InputValidatorTest {

    @Test
    void testIsValidPath() {
        assertTrue(InputValidator.isValidPath("/content/dam/my-fragment"));
        assertTrue(InputValidator.isValidPath("/content/we-retail/us/en"));
        assertFalse(InputValidator.isValidPath("/../etc/passwd"));
        assertFalse(InputValidator.isValidPath(null));
    }

    @Test
    void testIsValidName() {
        assertTrue(InputValidator.isValidName("dev"));
        assertTrue(InputValidator.isValidName("prod"));
        assertFalse(InputValidator.isValidName(""));
        assertFalse(InputValidator.isValidName(null));
    }

    @Test
    void testSanitizePath() {
        assertEquals("/content/dam/test", InputValidator.sanitizePath("/content/dam/test"));
    }

    @Test
    void testIsValidUrl() {
        assertTrue(InputValidator.isValidUrl("https://example.com"));
        assertFalse(InputValidator.isValidUrl("not-a-url"));
        assertFalse(InputValidator.isValidUrl("ftp://example.com"));
    }
}
