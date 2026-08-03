package com.example.eyal.rest.security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionUtils {
    private static final Logger log = LoggerFactory.getLogger(EncryptionUtils.class);
    private static final String ALGORITHM = "AES";
    private static final byte[] KEY = "AntigravityKey12".getBytes(StandardCharsets.UTF_8);

    public static String encrypt(String plainText) {
        log.debug("encrypt called with text length: {}", plainText != null ? plainText.length() : 0);
        try {
            SecretKeySpec secretKey = new SecretKeySpec(KEY, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            String result = Base64.getEncoder().encodeToString(encryptedBytes);
            log.debug("encrypt execution succeeded.");
            return result;
        } catch (Exception e) {
            log.error("Exception in EncryptionUtils.encrypt: {}", e.getMessage(), e);
            throw new RuntimeException("Error occurred during encryption", e);
        }
    }

    public static String decrypt(String encryptedText) {
        log.debug("decrypt called with cipher text length: {}", encryptedText != null ? encryptedText.length() : 0);
        try {
            SecretKeySpec secretKey = new SecretKeySpec(KEY, ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            String result = new String(decryptedBytes, StandardCharsets.UTF_8);
            log.debug("decrypt execution succeeded.");
            return result;
        } catch (Exception e) {
            log.error("Exception in EncryptionUtils.decrypt: {}", e.getMessage(), e);
            throw new RuntimeException("Error occurred during decryption", e);
        }
    }
}
