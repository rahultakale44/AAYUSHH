package com.aayush.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Password Service
 * 
 * Responsible for secure password hashing and verification using BCrypt
 * 
 * Security Features:
 * - BCrypt with strength 12 (2^12 = 4096 rounds)
 * - Random salt per password
 * - Secure comparison to prevent timing attacks
 * - No plaintext password storage
 * 
 * Important Notes:
 * - Password complexity validation is handled by DTOs (RegisterRequest)
 * - This service only handles encoding and verification
 * - Never logs passwords or encoded hashes
 * - Rejects null and blank passwords
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    /**
     * Encode raw password using BCrypt
     * 
     * BCrypt automatically:
     * - Generates a random salt
     * - Uses configured strength (12 rounds)
     * - Returns encoded hash in BCrypt format: $2a$12$...
     * 
     * @param rawPassword Plain text password to encode
     * @return BCrypt encoded password hash
     * @throws IllegalArgumentException if rawPassword is null or blank
     */
    public String encodePassword(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        
        if (rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Verify raw password matches encoded password hash
     * 
     * Uses secure comparison to prevent timing attacks
     * 
     * @param rawPassword Plain text password to verify
     * @param encodedPassword BCrypt encoded password hash
     * @return true if password matches, false otherwise
     * @throws IllegalArgumentException if rawPassword or encodedPassword is null
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Raw password cannot be null");
        }
        
        if (encodedPassword == null) {
            throw new IllegalArgumentException("Encoded password cannot be null");
        }
        
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException e) {
            // Malformed encoded hash
            log.debug("Password verification failed: Malformed encoded hash");
            return false;
        } catch (Exception e) {
            // Other BCrypt exceptions
            log.error("Password verification failed: Unexpected error", e);
            return false;
        }
    }
}
