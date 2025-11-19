package com.springpay.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Utility class for hashing and verifying passwords using BCrypt.
 * Uses cost factor of 12 for enhanced security.
 */
@Component
public class PasswordHasher {

    /**
     * BCrypt cost factor (number of hashing rounds = 2^12 = 4096).
     * Higher cost = more secure but slower. 12 is a good balance as of 2025.
     */
    private static final int BCRYPT_COST_FACTOR = 12;

    private final BCryptPasswordEncoder encoder;

    /**
     * Constructor initializes BCrypt encoder with cost factor 12.
     */
    public PasswordHasher() {
        this.encoder = new BCryptPasswordEncoder(BCRYPT_COST_FACTOR);
    }

    /**
     * Hashes a plain-text password using BCrypt.
     *
     * @param plainPassword the plain-text password
     * @return the BCrypt hashed password
     * @throws IllegalArgumentException if password is null or empty
     */
    public String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return encoder.encode(plainPassword);
    }

    /**
     * Verifies a plain-text password against a BCrypt hash.
     *
     * @param plainPassword the plain-text password to verify
     * @param hashedPassword the BCrypt hashed password
     * @return true if the password matches, false otherwise
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            throw new IllegalArgumentException("Hashed password cannot be null or empty");
        }
        return encoder.matches(plainPassword, hashedPassword);
    }
}
