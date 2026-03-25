package com.aemtools.aem.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class CredentialEncryption {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static byte[] MASTER_KEY;

    static {
        String keyEnv = System.getProperty("AEM_API_MASTER_KEY");
        if (keyEnv != null && keyEnv.length() >= 32) {
            MASTER_KEY = keyEnv.substring(0, 32).getBytes(StandardCharsets.UTF_8);
        } else {
            MASTER_KEY = generateDefaultKey();
        }
    }

    private static byte[] generateDefaultKey() {
        String keyFile = System.getProperty("user.home") + "/.aem-api/.key";
        java.io.File f = new java.io.File(keyFile);
        if (f.exists()) {
            try {
                return java.nio.file.Files.readAllBytes(f.toPath());
            } catch (Exception e) {
                System.err.println("Warning: Could not read master key file");
            }
        }
        byte[] key = new byte[32];
        SECURE_RANDOM.nextBytes(key);
        try {
            java.io.File parent = f.getParentFile();
            if (parent != null && !parent.mkdirs() && !parent.exists()) {
                System.err.println("Warning: Could not create master key directory");
            }
            java.nio.file.Files.write(f.toPath(), key);
            f.setReadable(true, false);
        } catch (Exception e) {
            System.err.println("Warning: Could not save master key file");
        }
        return key;
    }

    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            SecretKey key = new SecretKeySpec(MASTER_KEY, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return encrypted;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);

            SecretKey key = new SecretKeySpec(MASTER_KEY, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public static boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length > GCM_IV_LENGTH;
        } catch (Exception e) {
            return false;
        }
    }
}
