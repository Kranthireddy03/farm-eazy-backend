package com.farmeazy.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class ApiPayloadCryptoService {

    private static final String AES_GCM_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.api.encryption.secret:}")
    private String encryptionSecret;

    private SecretKeySpec secretKeySpec;

    @PostConstruct
    public void init() {
        if (encryptionSecret == null || encryptionSecret.isBlank()) {
            throw new IllegalStateException("security.api.encryption.secret must be configured for API encryption");
        }

        byte[] secretBytes = encryptionSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("security.api.encryption.secret must be at least 32 characters");
        }

        byte[] keyBytes = new byte[32];
        System.arraycopy(secretBytes, 0, keyBytes, 0, 32);
        this.secretKeySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt API payload", ex);
        }
    }

    public String decrypt(String encryptedPayload) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedPayload);
            if (combined.length <= IV_LENGTH) {
                throw new IllegalArgumentException("Encrypted payload is invalid");
            }

            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherBytes = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(AES_GCM_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] decrypted = cipher.doFinal(cipherBytes);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to decrypt API payload", ex);
        }
    }
}
