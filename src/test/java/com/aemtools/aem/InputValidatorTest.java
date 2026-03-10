package com.aemtools.aem;

import com.aemtools.aem.security.InputValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for InputValidator.
 * Tests all validation methods with various inputs including edge cases.
 */
@DisplayName("InputValidator Tests")
class InputValidatorTest {

    @Nested
    @DisplayName("isValidPath Tests")
    class IsValidPathTests {

        @Test
        @DisplayName("Valid AEM content paths should be accepted")
        void testValidContentPaths() {
            assertTrue(InputValidator.isValidPath("/content/dam/my-fragment"));
            assertTrue(InputValidator.isValidPath("/content/we-retail/us/en"));
            assertTrue(InputValidator.isValidPath("/content/dam/projects/test_folder"));
            assertTrue(InputValidator.isValidPath("/apps/my-app/components/button"));
            assertTrue(InputValidator.isValidPath("/etc/designs/my-design"));
        }

        @Test
        @DisplayName("Paths with special allowed characters should be accepted")
        void testPathsWithSpecialCharacters() {
            assertTrue(InputValidator.isValidPath("/content/dam/file-name"));
            assertTrue(InputValidator.isValidPath("/content/dam/file_name"));
            assertTrue(InputValidator.isValidPath("/content/dam/file:name"));
            assertTrue(InputValidator.isValidPath("/content/dam/file.name"));
        }

        @Test
        @DisplayName("Path traversal attempts should be rejected")
        void testPathTraversalRejected() {
            assertFalse(InputValidator.isValidPath("/../etc/passwd"));
            assertFalse(InputValidator.isValidPath("/content/../../../etc/passwd"));
            assertFalse(InputValidator.isValidPath("/content/dam/.."));
            assertFalse(InputValidator.isValidPath(".."));
        }

        @Test
        @DisplayName("Null byte injection should be rejected")
        void testNullByteInjectionRejected() {
            assertFalse(InputValidator.isValidPath("/content/dam/test\u0000.txt"));
            assertFalse(InputValidator.isValidPath("/content\u0000/dam"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Null and empty paths should be rejected")
        void testNullAndEmptyRejected(String path) {
            assertFalse(InputValidator.isValidPath(path));
        }
    }

    @Nested
    @DisplayName("isValidName Tests")
    class IsValidNameTests {

        @ParameterizedTest
        @ValueSource(strings = {"dev", "prod", "staging", "local", "test_env", "my-project"})
        @DisplayName("Valid environment names should be accepted")
        void testValidNames(String name) {
            assertTrue(InputValidator.isValidName(name));
        }

        @ParameterizedTest
        @ValueSource(strings = {"test123", "env_01", "Project-A1"})
        @DisplayName("Alphanumeric names with allowed special chars should be accepted")
        void testAlphanumericNames(String name) {
            assertTrue(InputValidator.isValidName(name));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Null and empty names should be rejected")
        void testNullAndEmptyRejected(String name) {
            assertFalse(InputValidator.isValidName(name));
        }

        @ParameterizedTest
        @ValueSource(strings = {"test name", "test/name", "test:name", "test.name", "test@name"})
        @DisplayName("Names with invalid characters should be rejected")
        void testInvalidCharactersRejected(String name) {
            assertFalse(InputValidator.isValidName(name));
        }
    }

    @Nested
    @DisplayName("isValidUrl Tests")
    class IsValidUrlTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "https://example.com",
            "http://localhost:4502",
            "https://author.adobeaemcloud.com",
            "http://192.168.1.1:8080",
            "https://my-aem.adobeaemcloud.com/content"
        })
        @DisplayName("Valid HTTP/HTTPS URLs should be accepted")
        void testValidUrls(String url) {
            assertTrue(InputValidator.isValidUrl(url));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "ftp://example.com",
            "file:///etc/passwd",
            "javascript:alert(1)",
            "not-a-url",
            "//example.com"
        })
        @DisplayName("Non-HTTP URLs and invalid formats should be rejected")
        void testInvalidUrlsRejected(String url) {
            assertFalse(InputValidator.isValidUrl(url));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Null and empty URLs should be rejected")
        void testNullAndEmptyRejected(String url) {
            assertFalse(InputValidator.isValidUrl(url));
        }
    }

    @Nested
    @DisplayName("sanitizePath Tests")
    class SanitizePathTests {

        @Test
        @DisplayName("Valid paths should remain unchanged")
        void testValidPathsUnchanged() {
            assertEquals("/content/dam/test", InputValidator.sanitizePath("/content/dam/test"));
            assertEquals("/content/we-retail", InputValidator.sanitizePath("/content/we-retail"));
        }

        @Test
        @DisplayName("Path traversal sequences should be removed")
        void testPathTraversalRemoved() {
            assertEquals("/content/etc/passwd", InputValidator.sanitizePath("/content/../etc/passwd"));
            assertEquals("/etc/passwd", InputValidator.sanitizePath("/../etc/passwd"));
        }

        @Test
        @DisplayName("Null bytes should be removed")
        void testNullBytesRemoved() {
            assertEquals("/content/dam/test.txt", InputValidator.sanitizePath("/content/dam/test\u0000.txt"));
        }

        @Test
        @DisplayName("Null input should return null")
        void testNullInputReturnsNull() {
            assertNull(InputValidator.sanitizePath(null));
        }
    }

    @Nested
    @DisplayName("validatePath Tests")
    class ValidatePathTests {

        @Test
        @DisplayName("Valid paths should not throw exception")
        void testValidPathsNoException() {
            assertDoesNotThrow(() -> InputValidator.validatePath("/content/dam/test"));
            assertDoesNotThrow(() -> InputValidator.validatePath("/content/we-retail/en"));
        }

        @Test
        @DisplayName("Invalid paths should throw IllegalArgumentException")
        void testInvalidPathsThrowException() {
            assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validatePath("/../etc/passwd"));
            assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validatePath(null));
            assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validatePath(""));
        }

        @Test
        @DisplayName("Exception message should contain the invalid path")
        void testExceptionMessageContainsPath() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> InputValidator.validatePath("/../etc/passwd")
            );
            assertTrue(exception.getMessage().contains("/../etc/passwd"));
        }
    }

    @Nested
    @DisplayName("validateName Tests")
    class ValidateNameTests {

        @Test
        @DisplayName("Valid names should not throw exception")
        void testValidNamesNoException() {
            assertDoesNotThrow(() -> InputValidator.validateName("dev"));
            assertDoesNotThrow(() -> InputValidator.validateName("my_project-01"));
        }

        @Test
        @DisplayName("Invalid names should throw IllegalArgumentException")
        void testInvalidNamesThrowException() {
            assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateName("invalid name"));
            assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateName(null));
            assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateName(""));
        }
    }

    @Nested
    @DisplayName("validateUrl Tests")
    class ValidateUrlTests {

        @Test
        @DisplayName("Valid URLs should not throw exception")
        void testValidUrlsNoException() {
            assertDoesNotThrow(() -> InputValidator.validateUrl("https://example.com"));
            assertDoesNotThrow(() -> InputValidator.validateUrl("http://localhost:4502"));
        }

        @Test
        @DisplayName("Invalid URLs should throw IllegalArgumentException")
        void testInvalidUrlsThrowException() {
            assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateUrl("ftp://example.com"));
            assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateUrl(null));
            assertThrows(IllegalArgumentException.class,
                () -> InputValidator.validateUrl("not-a-url"));
        }
    }
}
