package com.aemtools.aem;

import com.aemtools.aem.security.CredentialEncryption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for CredentialEncryption.
 * Verifies encryption and decryption functionality.
 */
@DisplayName("CredentialEncryption Tests")
class CredentialEncryptionTest {

    @Nested
    @DisplayName("Encryption Tests")
    class EncryptionTests {

        @Test
        @DisplayName("Encrypted value should be different from original")
        void testEncryptedDifferentFromOriginal() {
            String original = "my-secret-password";
            String encrypted = CredentialEncryption.encrypt(original);

            assertNotEquals(original, encrypted);
        }

        @Test
        @DisplayName("Same input encrypted twice should produce different ciphertext (due to random IV)")
        void testSameInputDifferentCiphertext() {
            String original = "my-secret-password";
            String encrypted1 = CredentialEncryption.encrypt(original);
            String encrypted2 = CredentialEncryption.encrypt(original);

            assertNotEquals(encrypted1, encrypted2);
        }

        @Test
        @DisplayName("Empty string should be returned as is")
        void testEmptyStringReturnsEmpty() {
            String result = CredentialEncryption.encrypt("");
            assertEquals("", result);
        }

        @Test
        @DisplayName("Null should be returned as is")
        void testNullReturnsNull() {
            String result = CredentialEncryption.encrypt(null);
            assertNull(result);
        }

        @Test
        @DisplayName("Encrypted value should be Base64 encoded")
        void testEncryptedIsBase64() {
            String original = "test-value";
            String encrypted = CredentialEncryption.encrypt(original);

            assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(encrypted));
        }
    }

    @Nested
    @DisplayName("Decryption Tests")
    class DecryptionTests {

        @Test
        @DisplayName("Decrypted value should match original")
        void testRoundTrip() {
            String original = "my-secret-password";
            String encrypted = CredentialEncryption.encrypt(original);
            String decrypted = CredentialEncryption.decrypt(encrypted);

            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("Various strings should survive round-trip")
        void testVariousStringsRoundTrip() {
            String[] testValues = {
                "simple",
                "with spaces and !@#$%^&*()",
                "unicode: \u00E9\u00F1\u00FC",
                "json: {\"key\": \"value\"}",
                "long string ".repeat(100)
            };

            for (String original : testValues) {
                String encrypted = CredentialEncryption.encrypt(original);
                String decrypted = CredentialEncryption.decrypt(encrypted);
                assertEquals(original, decrypted, "Failed for: " + original);
            }
        }

        @Test
        @DisplayName("Empty string should be returned as is")
        void testEmptyStringReturnsEmpty() {
            String result = CredentialEncryption.decrypt("");
            assertEquals("", result);
        }

        @Test
        @DisplayName("Null should be returned as is")
        void testNullReturnsNull() {
            String result = CredentialEncryption.decrypt(null);
            assertNull(result);
        }

        @Test
        @DisplayName("Invalid encrypted string should throw exception")
        void testInvalidEncryptedThrows() {
            assertThrows(RuntimeException.class,
                () -> CredentialEncryption.decrypt("not-valid-base64!!!"));
        }
    }

    @Nested
    @DisplayName("isEncrypted Tests")
    class IsEncryptedTests {

        @Test
        @DisplayName("Encrypted values should be detected as encrypted")
        void testEncryptedDetected() {
            String encrypted = CredentialEncryption.encrypt("test");
            assertTrue(CredentialEncryption.isEncrypted(encrypted));
        }

        @Test
        @DisplayName("Plain text should not be detected as encrypted")
        void testPlainTextNotDetected() {
            assertFalse(CredentialEncryption.isEncrypted("plain text"));
            assertFalse(CredentialEncryption.isEncrypted("simple"));
        }

        @Test
        @DisplayName("Empty string should not be detected as encrypted")
        void testEmptyNotDetected() {
            assertFalse(CredentialEncryption.isEncrypted(""));
        }

        @Test
        @DisplayName("Null should not be detected as encrypted")
        void testNullNotDetected() {
            assertFalse(CredentialEncryption.isEncrypted(null));
        }

        @Test
        @DisplayName("Short Base64 strings should not be detected as encrypted")
        void testShortBase64NotDetected() {
            // Base64 of very short strings won't be detected as encrypted
            // because they're shorter than IV length
            String shortBase64 = java.util.Base64.getEncoder().encodeToString("hi".getBytes());
            assertFalse(CredentialEncryption.isEncrypted(shortBase64));
        }
    }
}
