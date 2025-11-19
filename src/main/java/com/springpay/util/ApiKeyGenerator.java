package com.springpay.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Utility class for generating and hashing API keys.
 * Generates cryptographically secure random API keys (128+ bits).
 */
@Component
public class ApiKeyGenerator {

    /**
     * Length of generated API key in bytes (32 bytes = 256 bits = 64 hex characters).
     * This exceeds the minimum requirement of 128 bits (16 bytes).
     */
    private static final int KEY_LENGTH_BYTES = 32;

    /**
     * Prefix for API keys to identify them easily.
     */
    private static final String API_KEY_PREFIX = "sk_live_";

    private final SecureRandom secureRandom;

    /**
     * Constructor initializes SecureRandom for cryptographically strong random number generation.
     */
    public ApiKeyGenerator() {
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generates a new cryptographically secure API key.
     * Format: sk_live_{64 hex characters}
     * Total length: 72 characters (8 prefix + 64 hex)
     *
     * @return the generated plain-text API key
     */
    public String generateApiKey() {
        byte[] randomBytes = new byte[KEY_LENGTH_BYTES];
        secureRandom.nextBytes(randomBytes);

        String hexKey = HexFormat.of().formatHex(randomBytes);
        return API_KEY_PREFIX + hexKey;
    }

    /**
     * Hashes an API key using SHA-256.
     * The hash is stored in the database, never the plain-text key.
     *
     * @param apiKey the plain-text API key
     * @return the SHA-256 hash of the API key (hex encoded)
     * @throws IllegalArgumentException if apiKey is null or empty
     */
    public String hashApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in standard JDK
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies an API key against its stored hash.
     *
     * @param plainApiKey the plain-text API key to verify
     * @param storedHash the stored SHA-256 hash
     * @return true if the key matches the hash, false otherwise
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public boolean verifyApiKey(String plainApiKey, String storedHash) {
        if (plainApiKey == null || plainApiKey.isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        if (storedHash == null || storedHash.isEmpty()) {
            throw new IllegalArgumentException("Stored hash cannot be null or empty");
        }

        String computedHash = hashApiKey(plainApiKey);
        return computedHash.equals(storedHash);
    }
}
